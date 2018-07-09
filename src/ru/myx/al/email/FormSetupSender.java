/*
 * Created on 13.04.2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ru.myx.al.email;

import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.Engine;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
class FormSetupSender extends AbstractForm<FormSetupSender> {
	private static final ControlFieldset<?>	FIELDSET_SETTINGS	= ControlFieldset
																		.createFieldset( "settings" )
																		.addField( ControlFieldFactory
																				.createFieldString( "host",
																						MultivariantString
																								.getString( "SMTP host",
																										Collections
																												.singletonMap( "ru",
																														"Адрес SMTP сервера" ) ),
																						"localhost" ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "port",
																						MultivariantString
																								.getString( "SMTP port",
																										Collections
																												.singletonMap( "ru",
																														"Порт SMTP сервера" ) ),
																						25,
																						1,
																						65536 ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "timeout",
																						MultivariantString
																								.getString( "Connection timeput",
																										Collections
																												.singletonMap( "ru",
																														"Время ожидания соединения" ) ),
																						20,
																						5,
																						240 ) )
																		.addField( ControlFieldFactory
																				.createFieldBoolean( "plain",
																						MultivariantString
																								.getString( "send messages in text/plain format by default",
																										Collections
																												.singletonMap( "ru",
																														"Отправлять по умолчанию письма в формате text/plain" ) ),
																						false ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "max_attempts",
																						MultivariantString
																								.getString( "Max. attempts",
																										Collections
																												.singletonMap( "ru",
																														"Число попыток" ) ),
																						3 )
																				.setFieldType( "select" )
																				.setAttribute( "lookup",
																						new ru.myx.ae3.control.ControlLookupStatic( "1,1 attempt|2,2 attempts|3,3 attempts|5,5 attempts|10,10 attempts|25,25 attempts|100,100 attempts",
																								"|",
																								"," ) ) )
																		.addField( ControlFieldFactory
																				.createFieldString( "least_sender",
																						MultivariantString
																								.getString( "Least sender",
																										Collections
																												.singletonMap( "ru",
																														"Аварийный отправитель" ) ),
																						"invalid@" + Engine.HOST_NAME )
																				.setFieldHint( MultivariantString
																						.getString( "An address to use as a sender address when SMTP server rejects an original sender address.\r\nWARNING: SMTP server should never reject the address you provide in this field!",
																								Collections
																										.singletonMap( "ru",
																												"Почтовый адрес отправителя для использования в тех случаях, когда почтовый сервер не принимает исходный адрес отправителя.\r\nВНИМАНИЕ: Почтовый сервер никогда не должен отклонять указанный адрес!" ) ) ) );
	
	private final Plugin					parent;
	
	private static final ControlCommand<?>	CMD_SAVE			= Control.createCommand( "save", " OK " )
																		.setCommandPermission( "control" )
																		.setCommandIcon( "command-save" );
	
	FormSetupSender(final Plugin parent) {
		this.parent = parent;
		this.setData( parent.getSettingsProtected() );
		this.setAttributeIntern( "id", "setup" );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "SMTP client settings",
						Collections.singletonMap( "ru", "Настройки клиента SMTP" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == FormSetupSender.CMD_SAVE) {
			this.parent.update( this.getData() );
			return null;
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		return Control.createOptionsSingleton( FormSetupSender.CMD_SAVE );
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return FormSetupSender.FIELDSET_SETTINGS;
	}
	
}
