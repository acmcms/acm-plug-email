package ru.myx.al.email;

import java.util.Collections;

import ru.myx.ae1.access.Access;
import ru.myx.ae1.control.AbstractNode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae1.provide.FormStatusFiller;
import ru.myx.ae3.access.AccessPermissions;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.status.StatusFiller;
import ru.myx.ae3.status.StatusInfo;

// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////
// //////////////////////////////////////////////////////////////////////////////

final class NodeRoot extends AbstractNode {
	final Plugin							parent;
	
	private final StatusFiller				summary			= new StatusFiller() {
																@Override
																public void statusFill(final StatusInfo data) {
																	NodeRoot.this.parent.statusFill( data );
																	int stConnects = 0;
																	int stAttempts = 0;
																	int stSend = 0;
																	int stDiscarded = 0;
																	int stFailed = 0;
																	int stRequeued = 0;
																	for (int i = NodeRoot.this.parent.tasks.length - 1; i >= 0; --i) {
																		stConnects += NodeRoot.this.parent.tasks[i].stConnects;
																		stAttempts += NodeRoot.this.parent.tasks[i].stAttempts;
																		stSend += NodeRoot.this.parent.tasks[i].stSend;
																		stDiscarded += NodeRoot.this.parent.tasks[i].stDiscarded;
																		stFailed += NodeRoot.this.parent.tasks[i].stFailed;
																		stRequeued += NodeRoot.this.parent.tasks[i].stRequeued;
																	}
																	data.put( "Connects", stConnects );
																	data.put( "Attempts", stAttempts );
																	data.put( "Sent", stSend );
																	data.put( "Discarded", stDiscarded );
																	data.put( "Fails", stFailed );
																	data.put( "Requeued", stRequeued );
																}
															};
	
	private static final Object				STR_NODE_TITLE	= MultivariantString
																	.getString( "Email sender", Collections
																			.singletonMap( "ru", "Отправщик писем" ) );
	
	private static final ControlCommand<?>	CMD_SETUP		= Control
																	.createCommand( "setup",
																			MultivariantString
																					.getString( "SMTP client settings...",
																							Collections
																									.singletonMap( "ru",
																											"Настройки клиента SMTP" ) ) )
																	.setCommandPermission( "setup" )
																	.setCommandIcon( "command-setup" );
	
	private static final ControlCommand<?>	CMD_STATUS		= Control
																	.createCommand( "sstat",
																			MultivariantString
																					.getString( "Status (summary)",
																							Collections
																									.singletonMap( "ru",
																											"Обобщенный статус" ) ) )
																	.setCommandPermission( "view" )
																	.setCommandIcon( "command-status" );
	
	NodeRoot(final Plugin Parent) {
		this.parent = Parent;
	}
	
	@Override
	public AccessPermissions getCommandPermissions() {
		return Access
				.createPermissionsLocal()
				.addPermission( "view",
						MultivariantString.getString( "View SMTP client status and settings",
								Collections.singletonMap( "ru", "Просмотр статуса и настроек клиента SMTP" ) ) )
				.addPermission( "setup",
						MultivariantString.getString( "Modify SMTP client settings",
								Collections.singletonMap( "ru", "Изменение настроек клиента SMTP" ) ) );
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == NodeRoot.CMD_SETUP) {
			return new FormSetupSender( this.parent );
		}
		if (command == NodeRoot.CMD_STATUS) {
			return FormStatusFiller.createFormStatusFiller( MultivariantString.getString( "SMTP sender status (summary)",
					Collections.singletonMap( "ru", "Обобщенный статус клиента SMTP" ) ), this.summary );
		}
		if (command.getKey().endsWith( "_estat" )) {
			final int i = Integer.parseInt( command.getKey().substring( 0, command.getKey().indexOf( '_' ) ) );
			return FormStatusFiller.createFormStatusFiller( MultivariantString.getString( "SMTP sender status ("
					+ (i + 1)
					+ "/"
					+ this.parent.tasks.length
					+ ")",
					Collections.singletonMap( "ru", "Статус ("
							+ (i + 1)
							+ "/"
							+ this.parent.tasks.length
							+ ") клиента SMTP" ) ),
					this.parent.tasks[i] );
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( NodeRoot.CMD_STATUS );
		for (int i = 0; i < this.parent.tasks.length; ++i) {
			result.add( Control.createCommand( i + "_estat",
					MultivariantString.getString( "Status (" + (i + 1) + "/" + this.parent.tasks.length + ")",
							Collections
									.singletonMap( "ru", "Статус (" + (i + 1) + "/" + this.parent.tasks.length + ")" ) ) ) );
		}
		result.add( NodeRoot.CMD_SETUP );
		return result;
	}
	
	@Override
	public String getKey() {
		return "smtp_client";
	}
	
	@Override
	public String getTitle() {
		return NodeRoot.STR_NODE_TITLE.toString();
	}
}
