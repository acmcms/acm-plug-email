/*
 * Created on 14.06.2004
 */
package ru.myx.al.email;

import java.util.Collections;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractActor;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class ActorPersonal extends AbstractActor<ActorPersonal> {
	
	private static final ControlCommand<?>	CMD_SEND_SMTP_PLAIN	= Control
																		.createCommand( "sendmessagesmtpplain",
																				MultivariantString
																						.getString( "Send message: SMTP",
																								Collections
																										.singletonMap( "ru",
																												"Отправка сообщения: SMTP" ) ) )
																		.setCommandPermission( "sendmessagesmtp" )
																		.setCommandIcon( "command-send-message-smtp" );
	
	private static final ControlCommand<?>	CMD_SEND_SMTP_HTML	= Control
																		.createCommand( "sendmessagesmtphtml",
																				MultivariantString
																						.getString( "Send message: SMTP, HTML)",
																								Collections
																										.singletonMap( "ru",
																												"Отправка сообщения: SMTP, HTML" ) ) )
																		.setCommandPermission( "sendmessagesmtp" )
																		.setCommandIcon( "command-send-message-smtp" );
	
	@Override
	public AccessPermissions getCommandPermissions() {
		return Access.createPermissionsLocal().addPermission( "sendmessagesmtp",
				MultivariantString.getString( "Send messages (SMTP)",
						Collections.singletonMap( "ru", "Отправлять сообщения (SMTP)" ) ) );
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == ActorPersonal.CMD_SEND_SMTP_HTML) {
			return new FormSendMessageSmtp( false );
		} else if (command == ActorPersonal.CMD_SEND_SMTP_PLAIN) {
			return new FormSendMessageSmtp( true );
		} else {
			throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
		}
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( ActorPersonal.CMD_SEND_SMTP_HTML );
		result.add( ActorPersonal.CMD_SEND_SMTP_PLAIN );
		return result;
	}
}
