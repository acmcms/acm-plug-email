package ru.myx.al.email;

import java.io.IOException;
import java.util.Set;
import java.util.StringTokenizer;

import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseNativeArray;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.email.Email;
import ru.myx.ae3.email.EmailSender;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.help.Create;
import ru.myx.ae3.report.Report;

/**
 * Title: EmailSending plugin for CM3 Description: Copyright: Copyright (c) 2001
 * 
 * @author Alexander I. Kharitchev
 * @version 1.0
 */
final class SenderSimple implements EmailSender {
	
	private final Plugin parent;
	
	SenderSimple(final Plugin parent) {
		this.parent = parent;
	}
	
	@Override
	public final boolean sendEmail(final Email email) {
		
		final BaseObject original = email.baseGet("To", BaseObject.UNDEFINED);
		assert original != null : "NULL java value";
		if (original == BaseObject.UNDEFINED) {
			Report.error("SMTP", "No recipient address, skipping!");
			return false;
		}
		// Have to do it before message is copied to heaps of messages
		try {
			SenderTask.fixAttachments(email);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final BaseArray targets;
		{
			final BaseArray array = original.baseArray();
			if (array != null && !array.baseIsPrimitive()) {
				targets = array;
			} else {
				targets = new BaseNativeArray(original);
			}
		}
		final Set<String> recipients = Create.tempSet();
		final int length = targets.length();
		for (int j = 0; j < length; j++) {
			final String string = targets.baseGet(j, BaseObject.UNDEFINED).baseToJavaString();
			if (string == null) {
				continue;
			}
			for (final StringTokenizer st = new StringTokenizer(string, ",;", false); st.hasMoreTokens();) {
				final String recipient = st.nextToken().trim();
				if (recipient.length() == 0) {
					continue;
				}
				if (recipient.charAt(0) == '#') {
					if (recipient.startsWith("#email:")) {
						recipients.add(string.substring(7).trim());
						continue;
					}
					boolean clean = true;
					for (int i = recipient.length() - 1; i > 0; --i) {
						final char c = recipient.charAt(i);
						if (c != '-' && c != '.' && !Character.isJavaIdentifierPart(c)) {
							Report.error("SMTP", "Wrong list name, skipping: " + recipient.substring(1));
							clean = false;
							break;
						}
					}
					if (!clean) {
						continue;
					}
					final BaseObject o = Evaluate.evaluateObject(
							"Recipients && Recipients.getGroupRecipients( key = \"" + recipient.substring(1) + "\" )",
							Exec.createProcess(this.parent.getServer().getRootContext(), "SMTP recipients evaluation context"),
							null);
					assert o != null : "NULL java value";
					if (o == BaseObject.UNDEFINED || o == BaseObject.NULL) {
						continue;
					}
					final BaseArray array = o.baseArray();
					if (array != null && !array.baseIsPrimitive()) {
						final int count = array.length();
						for (int i = 0; i < count; ++i) {
							recipients.add(array.baseGet(i, BaseObject.UNDEFINED).baseToJavaString());
						}
						continue;
					}
					recipients.add(o.baseToJavaString());
					continue;
				}
				recipients.add(recipient);
			}
		}
		if (!recipients.isEmpty()) {
			for (final String current : recipients) {
				final BaseObject newMail = new BaseNativeObject();
				newMail.baseDefineImportAllEnumerable(email);
				newMail.baseDefine("To", current);
				this.parent.enqueueEmail(newMail);
			}
		}
		return true;
	}
}
