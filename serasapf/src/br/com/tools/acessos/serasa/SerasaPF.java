/**
 * $Revision: 1.2 $
 */
package br.com.tools.acessos.serasa;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import br.com.tools.acessos.Acesso;
import br.com.tools.acessos.exception.ConfigException;
import br.com.tools.acessos.exception.InfraException;
import br.com.tools.acessos.exception.LayoutException;
import br.com.tools.acessos.serasa.https.SerasaHttps;
import br.com.tools.acessos.serasa.util.SerasaUtil;
import br.com.tools.acessos.util.Util;

/**
 * @brief	Classe de acesso ao SERASA PF (Pessoa Fisica)
 * 			Serah gerado um JAR desta classe, sendo a mesma
 * 			chamada com: java -jar SeraraPF.jar
 * @author ccsilva
 *
 */
public class SerasaPF implements Acesso {
	
	/**
	 * @param <hashIn>
	 * @brief Pega o nome do Acesso; especializado em pegar o nome do acesso
	 * @param hashIn
	 */
	public HashMap<String, Object> execute( HashMap<String, Object> hashIn) throws InfraException, LayoutException, ConfigException {
		// definicao de contextos locais para logging
		Logger logger = SerasaUtil.getLogger();
		logger.debug(">> execute()");
		// essa variavel deve conter o PATH para todo o processo do acesso
		String url = System.getProperty("serasapf.datadir");
		if (url == null) {
			logger.error("");
			throw new ConfigException("Nao encontrou variavel de ambiente 'serasa.datadir'");
		}
		
		// Setand a vari�vel para acesso seguro
		System.setProperty("access.ssl", url+ "/resources/");		
		// nome padrao para o arquivo de propriedades
		String accessProperties = url + "/resources/serasa.properties";
		// nome padrao para o arquivo de layout B49c
		String layoutB49C = url + "/resources/layoutPF_b49c.xml";
		// nome padrao para o arquivo de layout P006
		String layoutP006 = url + "/resources/layout_p006.xml";
		// Armazena os dados de login no serasa
		StringBuffer serasaLogin = new StringBuffer("");
		// Armazena os dados que serao passados para o serasa
		StringBuffer serasaRequest = new StringBuffer(""); 
		// hashtable para retornar os registros formatados <K,V>
		HashMap<String,Object> hashOut = new HashMap<String, Object>();
		// arquivo de properties (serasa.properties)
		Properties pArquivo = new Properties();
		// inputstream para o arquivo de propriedades
		InputStream iProp;
		try {
			iProp = new BufferedInputStream(new FileInputStream(accessProperties));
		} catch (FileNotFoundException e1) {
			logger.error(e1);
			throw new ConfigException("O arquivo de propriedade [" +  accessProperties + "] n�o foi localizado;");
		}
		// Classe de utilidades serasa
		SerasaUtil serUtil = new SerasaUtil();
		
		/**
		 * Abrir e tratar o arquivo de properties... 
		 * Obs.: aqui teriamos a opcao de chamar metodo para a montagem dess string; seria trocar
		 * seis por meia duzia; se algum dsv organizado supor isso, que o implemente (tantan)
		 */
		// carrega o arquivo de propriedades
		try {
			pArquivo.load(iProp);
		} catch (IOException e) {
			logger.error(e);
			throw new ConfigException("Nao foi possivel abrir properties: [" +  accessProperties + "]");
		}
		// monta um header de login baseado num arquivo de properties
		try {
			serUtil.fillerLoginSerasa(accessProperties, serasaLogin );
		} catch (Exception e) {
			logger.error(e);
			throw new LayoutException("Erro no Filling do login SERASA-PF");
		}
		
		// Pega a chave para consulta simples; tem que ser "CPF"
		String sCPF = (String)hashIn.get("CPF");
		if ( sCPF == null)
			throw new LayoutException("Erro GRAVE: nao foi encontrado a key 'CPF' na Hash In");
			
		// monta o buffer de request para o SERASA
		try {
			serUtil.fillerRequestSerasa(layoutB49C, serasaRequest, sCPF );
		} catch (Exception e) {
			logger.error(e);
			throw new LayoutException("Erro no Filling do 'layout_B49C'");
		}
		// verifica se a consulta P005 est� ligada pelo campo "P005" = false
		boolean sP005 = false;
		if (hashIn.containsKey("P005")) {
			sP005 = hashIn.get("P005").equals(Boolean.FALSE ) ? false : true;
		}
		
		// P005 - Parcelas com Cheques de Compromisso
		// observe que qualquer outro caso, tipo for 'false', 'branco'
		// ou a propriedade nao existir, isso nao irah consultar P005
		if (sP005 != false) {
			// encontrei o P005 como 'true'
			try {
				serUtil.fillerLayoutP005(serasaRequest, hashIn );
			} catch (Exception e) {
				logger.error(e);
				throw new LayoutException("Erro no Filling do 'layout_P005'");
			}
		}
		// a montagem do layout P006 eh sempre obrigatorio
		try {
			serUtil.fillerRequestSerasa(layoutP006, serasaRequest, null );
		} catch (Exception e) {
			logger.error(e);
			throw new LayoutException("Erro no Filling do 'layout_P006'");
		}
		// Identificador de FIM de dados de envio
        // Devido a ser um registro fixo, nao houve necessidade de inclui-lo no layout
		serasaRequest.append(Util.stringFiller("T999", 116, "S" ));
		logger.info( ">>envio serasa: [" + serasaLogin + serasaRequest + "]");
		// cria contexto de consulta ao SERASA usando https WSDL
		SerasaHttps consultaHttps = new SerasaHttps();
		// monta hash de saida, baseado na string de retorno do SERASA
		try { // consulta e constroi hash de saida
			hashOut = serUtil.builderHashOut(consultaHttps.acessoSerasaHttps(url,serasaLogin.toString() + serasaRequest.toString()));
		} catch (Exception e) {
			logger.error(e);
			throw new InfraException("Erro no acesso ao SERASA-PF");
		}

		logger.debug( "<< execute()");
		// retorna a hash preenchida
		return hashOut;
	}
}

