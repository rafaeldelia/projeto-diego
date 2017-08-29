package br.com.tools.acessos.serasa.testes;

import java.util.HashMap;
import java.util.Iterator;

import br.com.tools.acessos.exception.ConfigException;
import br.com.tools.acessos.exception.InfraException;
import br.com.tools.acessos.exception.LayoutException;
import br.com.tools.acessos.serasa.SerasaPF;

import com.toolssoftware.framework.app.MainApp;
import com.toolssoftware.framework.log4j.Logger;

public class TesteSerasaPF extends MainApp {

	private String CPF;

	@Override
	public void init(Object[] arg0) {
		if (arg0.length <= 0) {
			System.out.println("O CPF e/ou cheque devem ser informados...");
			System.exit(-1);
		}

		this.CPF = (String) arg0[0];

	}

	protected void initFW() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TesteSerasaPF teste = new TesteSerasaPF();
		teste.run(args);
	}

	@Override
	public void destroy() {
	}

	@Override
	public boolean execute() {
		return process();
	}

	private boolean process() {
		Logger log = Logger.getLogger("Teste-Serasa-PF");
		log.trace(">>TesteSerasaPF.process()");

		log.info("Iniciando a execução do acesso ao Serasa para o CPF: ["
				+ this.CPF + "]");
		boolean execOk = true;
		HashMap<String, Object> serIn = new HashMap<String, Object>();
		HashMap<String, Object> serOut = new HashMap<String, Object>();
		SerasaPF ser = new SerasaPF();

		// filling the hashtable
		serIn.put("CPF", this.CPF);
		// deve ser informado; ele avisa true/false para a consulta ao P005
		serIn.put("P005", Boolean.FALSE);
		try {
			serOut = ser.execute(serIn);
			//System.out.println("Boi= " + serOut.get("B001_QTDCPFGRAFIA"));

			// trata de cuspir os marimbondos!
			Iterator<String> ele = serOut.keySet().iterator();
			// vomit out the hash contents
			while (ele.hasNext()) {
				String valueHash = (String) ele.next();
/*				System.out.println("[" + valueHash + "] >> ["
						+ serOut.get(valueHash) + "]");
*/
				log.info("<" + valueHash + ">"
						+ ((String)serOut.get(valueHash)).trim() + "</" +valueHash+">");
			}
		} catch (InfraException e) {
			log.error("O serviço encontra-se indisponível");
			log.printStackTrace(e);
		} catch (LayoutException e) {
			log.error("Existe diferença entre o layout enviado ao Serasa e o esperado pelo órgão");
			log.printStackTrace(e);
		} catch (ConfigException e) {
			log.error("Existe um erro na configuração do serviço: " + e.getMessage());
			log.printStackTrace(e);
		} finally {
			log.trace("<<TesteSerasaPF.process()");
			log.info("Finalizou a execução do acesso ao Serasa para o CPF: [" + this.CPF + "]");
		}

		return execOk;
	}

	@Override
	public void exit() {
		// TODO Auto-generated method stub

	}

}
