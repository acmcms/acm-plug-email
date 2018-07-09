/**
 * 
 */
package ru.myx.al.email;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractContainer;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.al.email.FormSendMessageSmtp.FormUploadAttachment;

class ContainerAttachments extends AbstractContainer<ContainerAttachments> {
	private final BaseObject				attachments;
	
	private static final ControlCommand<?>	CMD_ADD		= Control.createCommand( "add",
																MultivariantString.getString( "Add",
																		Collections.singletonMap( "ru", "Добавить" ) ) )
																.setCommandIcon( "command-create-add" );
	
	private static final ControlCommand<?>	CMD_CLEAR	= Control.createCommand( "clear",
																MultivariantString.getString( "Clear",
																		Collections.singletonMap( "ru", "Очистить" ) ) )
																.setCommandIcon( "command-delete-clear" );
	
	ContainerAttachments(final BaseObject attachments) {
		this.attachments = attachments;
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == ContainerAttachments.CMD_ADD) {
			return new FormUploadAttachment( this.attachments );
		}
		if (command == ContainerAttachments.CMD_CLEAR) {
			this.attachments.baseClear();
			return null;
		}
		if ("remove".equals( command.getKey() )) {
			final String key = Base.getString( command.getAttributes(), "key", null );
			this.attachments.baseDelete( key );
			return null;
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( ContainerAttachments.CMD_ADD );
		if (Base.hasKeys( this.attachments )) {
			result.add( ContainerAttachments.CMD_CLEAR );
		}
		return result;
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		return Control.createOptionsSingleton( Control
				.createCommand( "remove",
						MultivariantString.getString( "Delete", Collections.singletonMap( "ru", "Удалить" ) ) )
				.setCommandIcon( "command-delete" ).setAttribute( "key", key ) );
	}
}
