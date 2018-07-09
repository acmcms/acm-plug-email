import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseProperty;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.produce.Produce;
import ru.myx.sapi.MailSAPI;

/*
 * Created on 07.10.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class Main {
	
	/**
	 * @param args
	 */
	public static final void main(final String[] args) {
		System.out.println( "RU.MYX.AE1PLUG.MAIL: plugin: ACM [Mail] is being initialized..." );
		Produce.registerFactory( new PluginFactory() );
		ExecProcess.GLOBAL.baseDefine( "Mail", Base.forUnknown( new MailSAPI() ), BaseProperty.ATTRS_MASK_NNN );
	}
}
