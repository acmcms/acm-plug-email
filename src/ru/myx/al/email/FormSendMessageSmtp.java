/*
 * Created on 14.06.2004
 */
package ru.myx.al.email;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import ru.myx.ae1.BaseRT3;
import ru.myx.ae1.access.AccessUser;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArrayDynamic;
import ru.myx.ae3.base.BaseFunctionActAbstract;
import ru.myx.ae3.base.BaseHostLookup;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.base.BasePrimitive;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.email.Email;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.Convert.MapEntry;
import ru.myx.ae3.help.Format;

/** @author myx
 *
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments */
class FormSendMessageSmtp extends AbstractForm<FormSendMessageSmtp> {
	
	static class FormUploadAttachment extends AbstractForm<FormUploadAttachment> {
		
		private static final ControlFieldset<?> FIELDSET = ControlFieldset.createFieldset().addField(
				ControlFieldFactory.createFieldBinary("attachment", MultivariantString.getString("Attachment", Collections.singletonMap("ru", "Вложение")), Integer.MAX_VALUE));

		private static final ControlCommand<?> CMD_SAVE = Control.createCommand("ok", " OK ").setCommandIcon("command-save");

		private final BaseObject attachments;

		FormUploadAttachment(final BaseObject attachments) {
			this.attachments = attachments;
			this.setAttributeIntern("id", "add_attachment");
			this.setAttributeIntern("title", MultivariantString.getString("Add an attachment", Collections.singletonMap("ru", "Добавление вложения")));
			this.recalculate();
		}

		@Override
		public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
			
			if (command == FormUploadAttachment.CMD_SAVE) {
				final TransferCopier attachment = (TransferCopier) Base.getJava(this.getData(), "attachment", null);
				final String name = Base.getString(this.getData(), "attachment_contentname", null);
				final int size = Convert.MapEntry.toInt(this.getData(), "attachment_size", 0);
				this.attachments.baseDefine(name, new BasicAttachment(name, size, attachment));
				return null;
			}
			throw new IllegalArgumentException("Unknown command: " + command.getKey());
		}

		@Override
		public ControlCommandset getCommands() {
			
			return Control.createOptionsSingleton(FormUploadAttachment.CMD_SAVE);
		}

		@Override
		public ControlFieldset<?> getFieldset() {
			
			return FormUploadAttachment.FIELDSET;
		}
	}

	private static final String[] USER_KEYS = {
			"Name", "name", "Login", "login",
	};

	private static final BaseHostLookup LOOKUP_SIZE_FORMATTER = new BaseHostLookup() {
		
		@Override
		public BaseObject baseGetLookupValue(final BaseObject key) {
			
			return Base.forString(Format.Compact.toBytes(key.baseToJavaLong()));
		}

		@Override
		public boolean baseHasKeysOwn() {
			
			return false;
		}

		@Override
		public Iterator<String> baseKeysOwn() {
			
			return BaseObject.ITERATOR_EMPTY;
		}

		@Override
		public Iterator<? extends BasePrimitive<?>> baseKeysOwnPrimitive() {
			
			return BaseObject.ITERATOR_EMPTY_PRIMITIVE;
		}

		@Override
		public String toString() {
			
			return "[Lookup: Size Formatter]";
		}
	};

	private static final ControlFieldset<?> FIELDSET_ATTACHMENT_LISTING = ControlFieldset.createFieldset()
			.addField(ControlFieldFactory.createFieldString("name", MultivariantString.getString("Name", Collections.singletonMap("ru", "Имя")), "")).addField(
					ControlFieldFactory.createFieldInteger("size", MultivariantString.getString("Size", Collections.singletonMap("ru", "Размер")), 0)
							.setAttribute("lookup", FormSendMessageSmtp.LOOKUP_SIZE_FORMATTER));

	private static final ControlCommand<?> CMD_SEND = Control.createCommand("send", MultivariantString.getString("Send", Collections.singletonMap("ru", "Отправить")))
			.setCommandPermission("sendmessagesmtp").setCommandIcon("command-send-message-smtp");

	private final BaseArrayDynamic<ControlBasic<?>> attachments;

	private final ControlFieldset<?> fieldset;

	private final boolean plain;

	FormSendMessageSmtp(final boolean plain) {
		this.plain = plain;
		this.setAttributeIntern("id", "send_message_smtp");
		this.setAttributeIntern("title", plain
			? MultivariantString.getString("Send e-mail message over SMTP", Collections.singletonMap("ru", "Отправка электронного письма по SMTP"))
			: MultivariantString.getString("Send html e-mail message over SMTP", Collections.singletonMap("ru", "Отправка электронного письма в формате HTML по SMTP")));
		this.recalculate();
		this.attachments = BaseObject.createArray();
		final ContainerAttachments container = new ContainerAttachments(this.attachments);
		if (plain) {
			this.fieldset = ControlFieldset.createFieldset().addField(
					ControlFieldFactory.createFieldString("sender", MultivariantString.getString("Sender", Collections.singletonMap("ru", "Отправитель")), "").setConstant())
					.addField(
							ControlFieldFactory.createFieldString("recipient", MultivariantString.getString("Recipient", Collections.singletonMap("ru", "Получатель")), "")
									.setFieldHint(
											MultivariantString.getString(
													"Use ; or , symbols to enter multiple recipients",
													Collections.singletonMap("ru", "Используйте символы ; или , чтобы указать нескольких получателей"))))
					.addField(ControlFieldFactory.createFieldString("copy", MultivariantString.getString("Carbon Copy", Collections.singletonMap("ru", "Копия")), ""))
					.addField(ControlFieldFactory.createFieldString("blindcopy", MultivariantString.getString("Blind Copy", Collections.singletonMap("ru", "Скрытая копия")), ""))
					.addField(ControlFieldFactory.createFieldString("subject", MultivariantString.getString("Subject", Collections.singletonMap("ru", "Тема")), ""))
					.addField(
							ControlFieldFactory
									.createFieldString(
											"message",
											MultivariantString.getString("Message text", Collections.singletonMap("ru", "Текст сообщения")),
											"",
											0,
											128 * 1024)
									.setFieldType("text").setFieldVariant("bigtext"))
					.addField(
							Control.createFieldList("attachments", MultivariantString.getString("Attachments", Collections.singletonMap("ru", "Вложения")), this.attachments)
									.setAttribute("content_fieldset", FormSendMessageSmtp.FIELDSET_ATTACHMENT_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<Object, ContainerAttachments>(Object.class, ContainerAttachments.class) {
										
										@Override
										public ContainerAttachments apply(final Object argument) {
											
											return container;
										}
									}));
		} else {
			this.fieldset = ControlFieldset.createFieldset().addField(
					ControlFieldFactory.createFieldString("sender", MultivariantString.getString("Sender", Collections.singletonMap("ru", "Отправитель")), "").setConstant())
					.addField(
							ControlFieldFactory.createFieldString("recipient", MultivariantString.getString("Recipient", Collections.singletonMap("ru", "Получатель")), "")
									.setFieldHint(
											MultivariantString.getString(
													"Use ; or , symbols to enter multiple recipients",
													Collections.singletonMap("ru", "Используйте символы ; или , чтобы указать нескольких получателей"))))
					.addField(ControlFieldFactory.createFieldString("copy", MultivariantString.getString("Carbon Copy", Collections.singletonMap("ru", "Копия")), ""))
					.addField(ControlFieldFactory.createFieldString("blindcopy", MultivariantString.getString("Blind Copy", Collections.singletonMap("ru", "Скрытая копия")), ""))
					.addField(ControlFieldFactory.createFieldString("subject", MultivariantString.getString("Subject", Collections.singletonMap("ru", "Тема")), ""))
					.addField(
							ControlFieldFactory
									.createFieldString(
											"message",
											MultivariantString.getString("Message text", Collections.singletonMap("ru", "Текст сообщения")),
											"",
											0,
											128 * 1024)
									.setFieldType("text").setFieldVariant("htmltext"))
					.addField(
							Control.createFieldList("attachments", MultivariantString.getString("Attachments", Collections.singletonMap("ru", "Вложения")), this.attachments)
									.setAttribute("content_fieldset", FormSendMessageSmtp.FIELDSET_ATTACHMENT_LISTING)
									.setAttribute("content_handler", new BaseFunctionActAbstract<Object, ContainerAttachments>(Object.class, ContainerAttachments.class) {
										
										@Override
										public ContainerAttachments apply(final Object argument) {
											
											return container;
										}
									}));
		}
		final AccessUser<?> user = Context.getUser(Exec.currentProcess());
		final String name = MapEntry.anyToString(user.getProfile(), FormSendMessageSmtp.USER_KEYS, user.getLogin()).trim();
		final BaseObject data = new BaseNativeObject()//
				.putAppend(
						"sender", //
						name.length() > 0
							? '"' + name + '"' + " <" + user.getEmail() + '>'
							: user.getEmail())//
				.putAppend("attachments", this.attachments)//
		;
		this.setData(data);
	}

	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		
		if (command == FormSendMessageSmtp.CMD_SEND) {
			final Email email = new Email();
			final BaseObject params = this.getData();
			final String sender = Base.getString(params, "sender", "").trim();
			if (sender.length() > 0) {
				email.put("From", sender);
			}
			final String recipient = Base.getString(params, "recipient", "").trim();
			if (recipient.length() > 0) {
				email.put("To", recipient);
			}
			final String copy = Base.getString(params, "copy", "").trim();
			if (copy.length() > 0) {
				email.put("CC", copy);
			}
			final String blindCopy = Base.getString(params, "blindCopy", "").trim();
			if (blindCopy.length() > 0) {
				email.put("BCC", blindCopy);
			}
			final String subject = Base.getString(params, "subject", "").trim();
			if (subject.length() > 0) {
				email.put("Subject", subject);
			}
			final String message = Base.getString(params, "message", "").trim();
			if (message.length() > 0) {
				email.put("Body", message);
			}
			if (this.attachments.length() > 0) {
				final Map<String, Object> attach = new TreeMap<>();
				final int length = this.attachments.length();
				for (int i = 0; i < length; ++i) {
					final ControlBasic<?> basic = (ControlBasic<?>) this.attachments.baseGet(i, null);
					final BasicAttachment attachment = (BasicAttachment) basic;
					final String aname = attachment.getTitle();
					if (attach.containsKey(aname)) {
						for (int j = 0;; j++) {
							if (attach.containsKey(j + aname)) {
								continue;
							}
							attach.put(j + aname, attachment.getAttachment());
							break;
						}
					} else {
						attach.put(aname, attachment.getAttachment());
					}
				}
				email.put("Attachments", attach);
			}
			email.put("Format", this.plain
				? "plain"
				: "html");
			BaseRT3.runtime().getEmailSender().sendEmail(email);
			return null;
		}
		throw new IllegalArgumentException("Unknown command: " + command.getKey());
	}

	@Override
	public ControlCommandset getCommands() {
		
		return Control.createOptionsSingleton(FormSendMessageSmtp.CMD_SEND);
	}

	@Override
	public ControlFieldset<?> getFieldset() {
		
		return this.fieldset;
	}
}
