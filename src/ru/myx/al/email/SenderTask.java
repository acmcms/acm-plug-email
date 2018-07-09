package ru.myx.al.email;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

import mwm.email.BodyBinary;
import mwm.email.BodyBinaryAttachment;
import mwm.email.BodyBinaryInline;
import mwm.email.EmailAddress;
import mwm.email.FieldAbstract;
import mwm.email.FieldSimple;
import mwm.email.InvalidFormatException;
import mwm.email.Message;
import mwm.email.encoder.Base64Encoder;
import mwm.email.smtp.SMTPSender;
import mwm.email.util.MessageCreator;
import mwm.email.util.MessageCreatorMIME;
import mwm.email.util.MessageCreatorRFC822;
import ru.myx.ae1.know.Know;
import ru.myx.ae1.know.Server;
import ru.myx.ae3.Engine;
import ru.myx.ae3.act.Act;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BaseString;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.common.Value;
import ru.myx.ae3.email.Email;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.help.Html;
import ru.myx.ae3.help.Text;
import ru.myx.ae3.mime.MimeType;
import ru.myx.ae3.report.Report;
import ru.myx.ae3.status.StatusFiller;
import ru.myx.ae3.status.StatusInfo;

final class SenderTask implements Runnable, StatusFiller {
	
	private static final String OWNER = "RT3/SMTPCLIENT-SENDER";
	
	private static final String MAILER_STRING = "ACM.CMS (" + Know.systemVersion() + "/" + Know.systemBuild() + ")";
	
	private static final FieldAbstract MAILER_HEADER = new FieldSimple("X-Mailer", SenderTask.MAILER_STRING);
	
	private static final FieldAbstract AGENT_HEADER = new FieldSimple("User-Agent", SenderTask.MAILER_STRING);
	
	static final TransferCopier convertToCopier(final Object object) throws IOException {
		
		if (object instanceof File) {
			return Transfer.createCopier((File) object);
		}
		if (object instanceof byte[]) {
			return Transfer.createCopier((byte[]) object);
		}
		if (object instanceof String) {
			return Transfer.createCopier(((String) object).getBytes(Engine.CHARSET_UTF8));
		}
		if (object instanceof TransferBuffer) {
			return ((TransferBuffer) object).toBinary();
		}
		if (object instanceof TransferCopier) {
			return (TransferCopier) object;
		}
		if (object instanceof BaseMessage) {
			final BaseMessage msg = (BaseMessage) object;
			return msg.isBinary()
				? msg.toBinary().getBinary()
				: msg.isCharacter()
					? Transfer.createCopierUtf8(msg.toCharacter().getText())
					: msg.toBinary().getBinary();
		}
		{
			if (object instanceof Value<?>) {
				final Object baseValue = ((Value<?>) object).baseValue();
				if (baseValue != object) {
					return SenderTask.convertToCopier(baseValue);
				}
			}
			throw new IllegalArgumentException("Unsupported attachment type: " + object.getClass().getName());
		}
	}
	
	static final Message createMessage(final BaseObject email, final String leastSender, final boolean plainDefault) throws IOException {
		
		SenderTask.fixAttachments(email);
		final String sender = SenderTask.getNonEmptyString(email, "From", leastSender).trim();
		final String recipient = SenderTask.getNonEmptyString(email, "To", "").replace(',', ';').trim();
		final String subject = SenderTask.getNonEmptyString(email, "Subject", "no subject");
		final String carbonCopy = SenderTask.getNonEmptyString(email, "CC", "").replace(',', ';').trim();
		final String blindCarbonCopy = SenderTask.getNonEmptyString(email, "BCC", "").replace(',', ';').trim();
		final String reply = SenderTask.getNonEmptyString(email, "Reply", sender).trim();
		final String body = SenderTask.getNonEmptyString(email, "Body", "no body");
		final String encoding = SenderTask.getNonEmptyString(email, "Encoding", "").trim();
		final boolean plain = SenderTask.getNonEmptyString(
				email,
				"Format",
				plainDefault
					? "plain"
					: "html")
				.equalsIgnoreCase("plain");
		final Collection<BodyBinary> attachments = Convert.Any.toAny(Base.getJava(email, "AttachmentList", Collections.EMPTY_LIST));
		final Charset charset = encoding.length() == 0
			? Engine.CHARSET_UTF8
			: Charset.forName(encoding);
		final String useReply = reply == leastSender || reply.length() == 0
			? null
			: reply;
		final Message message;
		if (plain && attachments.isEmpty()) {
			final MessageCreatorRFC822 creatorRFC = new MessageCreatorRFC822(charset);
			SenderTask.setupMessageParameters(sender, leastSender, recipient, carbonCopy, blindCarbonCopy, useReply, creatorRFC);
			if (encoding.toLowerCase().startsWith("utf")) {
				creatorRFC.setSubject(subject);
				creatorRFC.setText(body);
			} else {
				creatorRFC.setSubject(SenderTask.narrowSymbols(subject));
				creatorRFC.setText(SenderTask.narrowSymbols(body));
			}
			message = creatorRFC.getMessage();
		} else {
			final MessageCreatorMIME creatorMIME = new MessageCreatorMIME(charset);
			SenderTask.setupMessageParameters(sender, leastSender, recipient, carbonCopy, blindCarbonCopy, useReply, creatorMIME);
			if (encoding.toLowerCase().startsWith("utf")) {
				creatorMIME.setSubject(subject);
				if (plain) {
					creatorMIME.setBodyText(body);
				} else {
					creatorMIME.setBodyHtml(body, Html.cleanHtml(body));
				}
			} else {
				creatorMIME.setSubject(SenderTask.narrowSymbols(subject));
				if (plain) {
					creatorMIME.setBodyText(SenderTask.narrowSymbols(body));
				} else {
					creatorMIME.setBodyHtml(SenderTask.narrowSymbols(body), SenderTask.narrowSymbols(Html.cleanHtml(body)));
				}
			}
			for (final BodyBinary current : attachments) {
				creatorMIME.addAttachment(current);
			}
			message = creatorMIME.getMessage();
			// only for MIME
			message.getMessageBody().getHeader().set(new FieldSimple("MIME-Version", "1.0"));
		}
		message.getMessageBody().getHeader().set(SenderTask.MAILER_HEADER);
		message.getMessageBody().getHeader().set(SenderTask.AGENT_HEADER);
		message.getMessageBody().getHeader().set(new FieldSimple("Message-ID", '<' + Engine.createGuid() + '@' + Context.getServer(Exec.currentProcess()).getDomainId() + '>'));
		return message;
	}
	
	/** occurs only one, marks the message
	 *
	 * don't care if text/plain message
	 *
	 * @param email
	 */
	static final void fixAttachments(final BaseObject email) throws IOException {
		
		if (Base.getBoolean(email, "isAttachmentsFixed", false)) {
			return;
		}
		email.baseDefine("isAttachmentsFixed", true);
		final BaseObject attachments = email.baseGet("Attachments", BaseObject.UNDEFINED);
		if (!Base.hasKeys(attachments)) {
			return;
		}
		final BaseArrayDynamic<BaseObject> attachmentList = BaseObject.createArray();
		email.baseDefine("AttachmentList", attachmentList);
		final String body = SenderTask.getNonEmptyString(email, "Body", "no body").trim();
		StringBuilder builder = null;
		for (final Iterator<String> iterator = Base.keys(attachments); iterator.hasNext();) {
			final String key = iterator.next();
			final String contentType = MimeType.forName(key, MimeType.SMT_APPLICATION_UNKNOWN);
			final TransferCopier data = SenderTask.convertToCopier(attachments.baseGet(key, BaseObject.UNDEFINED));
			final String pattern = "=\"cid:" + key + '"';
			final int check = builder == null
				? body.lastIndexOf(pattern)
				: builder.lastIndexOf(pattern);
			if (check >= 0) {
				final int patternLength = pattern.length();
				final String contentId = Engine.createGuid() + "." + key + "@embed.ded";
				final String replacement = "=\"cid:" + contentId + '"';
				if (builder == null) {
					builder = new StringBuilder(body);
				}
				attachmentList.add(Base.forUnknown(new BodyBinaryInline(data, key, contentId, contentType, new Base64Encoder())));
				for (int position = check; position >= 0;) {
					builder.replace(position, position + patternLength, replacement);
					position = builder.lastIndexOf(pattern, position - 1);
				}
			} else {
				attachmentList.add(Base.forUnknown(new BodyBinaryAttachment(data, key, contentType, new Base64Encoder())));
			}
		}
		if (builder != null) {
			email.baseDefine("Body", builder.toString());
		}
	}
	
	private static final String getNonEmptyString(final BaseObject map, final String name, final String defaultValue) {
		
		final BaseObject value = map.baseGet(name, BaseObject.UNDEFINED);
		return value == BaseObject.UNDEFINED || value == BaseObject.NULL || value == BaseString.EMPTY
			? defaultValue
			: value.baseToJavaString();
	}
	
	private static final String narrowSymbols(final String text) {
		
		assert text != null;
		StringBuilder result = null;
		for (int i = text.length() - 1; i >= 0; --i) {
			final char c = text.charAt(i);
			switch (c) {
				case 147 :
				case 148 :
				case 171 :
				case 187 :
				case 0x02DD :
				case 0x201C :
				case 0x201D :
				case 0x201E :
				case 0x2033 :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '"');
					break;
				case 150 :
				case 151 :
				case 0x2013 :
				case 0x2014 :
				case 0x2015 :
				case 0x2212 :
				case 0x2500 :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '-');
					break;
				case 186 :
				case 0x00B7 :
				case 0x2022 :
				case 0x2024 :
				case 0x2219 :
				case 0x25D8 :
				case 0x25E6 :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '-');
					break;
				case '№' :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '#');
					break;
				case '’' :
				case '`' :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '\'');
					break;
				case 0x2026 :
					if (result == null) {
						result = new StringBuilder(text);
					}
					result.setCharAt(i, '.');
					result.insert(i, "..");
					break;
				default :
			}
		}
		if (result == null) {
			return text;
		}
		return result.toString();
	}
	
	private static final void setupMessageParameters(final String sender,
			final String leastSender,
			final String recipient,
			final String carbonCopy,
			final String blindCarbonCopy,
			final String reply,
			final MessageCreator messageCreator) throws InvalidFormatException {

		try {
			messageCreator.setSender(EmailAddress.parseAddress(sender));
		} catch (final Throwable T) {
			messageCreator.setSender(EmailAddress.parseAddress(leastSender));
		}
		if (reply != null && reply.length() > 0) {
			try {
				messageCreator.setReplyTo(EmailAddress.parseAddress(reply));
			} catch (final Throwable T) {
				Report.warning(SenderTask.OWNER, "Wrong address: ReplyTo=" + reply);
			}
		}
		for (final StringTokenizer st = new StringTokenizer(recipient, ";"); st.hasMoreTokens();) {
			final String current = st.nextToken().trim();
			if (current.length() > 0) {
				try {
					messageCreator.addTo(EmailAddress.parseAddress(current));
				} catch (final Throwable t) {
					Report.warning(SenderTask.OWNER, "Wrong address: RCPT=" + current);
				}
			}
		}
		for (final StringTokenizer st = new StringTokenizer(carbonCopy, ";"); st.hasMoreTokens();) {
			final String current = st.nextToken().trim();
			if (current.length() > 0) {
				try {
					messageCreator.addCc(EmailAddress.parseAddress(current));
				} catch (final Throwable t) {
					Report.warning(SenderTask.OWNER, "Wrong address: CC=" + current);
				}
			}
		}
		for (final StringTokenizer st = new StringTokenizer(blindCarbonCopy, ";"); st.hasMoreTokens();) {
			final String current = st.nextToken().trim();
			if (current.length() > 0) {
				try {
					messageCreator.addBcc(EmailAddress.parseAddress(current));
				} catch (final Throwable t) {
					Report.warning(SenderTask.OWNER, "Wrong address: BCC=" + current);
				}
			}
		}
	}
	
	protected int stConnects;
	
	protected int stAttempts;
	
	protected int stSend;
	
	protected int stFailed;
	
	protected int stDiscarded;
	
	protected int stRequeued;
	
	private final Plugin parent;
	
	private final int index;
	
	private final ExecProcess ctx;
	
	protected mwm.email.smtp.SMTPSender messageSender = null;
	
	SenderTask(final Plugin parent, final int index) {
		
		this.parent = parent;
		this.index = index;
		final Server server = parent.getServer();
		this.ctx = Exec.createProcess(server.getRootContext(), "SMTP sender task (" + server.getZoneId() + ")");
	}
	
	@Override
	public void run() {
		
		try {
			for (; this.parent.tasks[this.index] == this;) {
				final BaseObject email = this.parent.getNext();
				if (email == null) {
					break;
				}
				final int retries = Convert.MapEntry.toInt(email, "Retries", Integer.MAX_VALUE);
				email.baseDefine( //
						"Retries",
						retries == Integer.MAX_VALUE
							? this.parent.maxAttempts
							: retries - 1);
				this.stAttempts++;
				if (this.send(email)) {
					this.stSend++;
				} else {
					this.stFailed++;
					if (retries == 1) {
						this.stDiscarded++;
					} else {
						this.parent.enqueueEmail(email);
						this.stRequeued++;
					}
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		} finally {
			if (this.parent.tasks[this.index] == this) {
				Act.later(this.ctx, this, this.index * 5000 + 5000);
			}
		}
	}
	
	private boolean send(final BaseObject email) {
		
		try {
			return this.sendImpl(SenderTask.createMessage(email, this.parent.leastSender, this.parent.plainDefault));
		} catch (final Throwable e) {
			final String sender = SenderTask.getNonEmptyString(email, "From", "").trim();
			final String recipient = SenderTask.getNonEmptyString(email, "To", "").replace(',', ';').trim();
			final String subject = SenderTask.getNonEmptyString(email, "Subject", "no subject");
			if ((Convert.MapEntry.toInt(email, "MessageTypeMask", 0) & Email.TYPE_MASK_IGNOREERRORS) == 0) {
				Report.error(
						SenderTask.OWNER,
						"      host       = " + this.parent.smtpHost + "\r\n" + "      port       = " + this.parent.smtpPort + "\r\n" + "      sender     = " + sender + "\r\n"
								+ "      recipient  = " + recipient + "\r\n" + "      subject    = " + subject + "\r\n" + "      error type = " + e.getClass().getName() + "\r\n"
								+ "      error      = " + e.getMessage());
			}
			Report.error(
					SenderTask.OWNER,
					"Failed: from=" + sender + ", to=" + recipient + ", subj=" + subject + ", err=" + (e.getMessage() == null
						? e.toString()
						: e.getMessage()) + ", full:" + Format.Throwable.toText(e));
			return false;
		}
	}
	
	private boolean sendImpl(final Message message) throws IOException {
		
		final SMTPSender sender;
		if (this.messageSender == null) {
			sender = new SMTPSender(this.parent.smtpHost, this.parent.smtpPort, Context.getServer(Exec.currentProcess()).getZoneId());
			sender.connect();
			this.messageSender = sender;
			this.stConnects++;
		} else {
			sender = this.messageSender;
			if (sender.isConnected()) {
				try {
					sender.checkConnection();
				} catch (final IOException e) {
					sender.disconnect();
					sender.connect();
					this.stConnects++;
				}
			} else {
				sender.connect();
				this.stConnects++;
			}
		}
		sender.send(message);
		Report.success(
				SenderTask.OWNER,
				"Sent: from=" + message.getOriginator() + ", to=" + Text.join(message.getRecipientHandler().getRecipients(), "; ") + ", subject=" + message.getSubject());
		return true;
	}
	
	void start() {
		
		Act.later(this.ctx, this, (long) (Math.random() * 10000 + 5000));
	}
	
	@Override
	public void statusFill(final StatusInfo data) {
		
		this.parent.statusFill(data);
		data.put("Connects", this.stConnects);
		data.put("Attempts", this.stAttempts);
		data.put("Sent", this.stSend);
		data.put("Discarded", this.stDiscarded);
		data.put("Fails", this.stFailed);
		data.put("Requeued", this.stRequeued);
	}
	
	@Override
	public String toString() {
		
		return "Email sender task";
	}
}
