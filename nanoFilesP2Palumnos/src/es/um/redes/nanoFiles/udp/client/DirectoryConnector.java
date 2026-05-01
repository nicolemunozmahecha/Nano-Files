package es.um.redes.nanoFiles.udp.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.application.Directory;
import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileDatabase;
import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DIRECTORY_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;
	// HAY QUE DECLARAR EL NUMERO MAXIMO DE BYTES PARA EL ARRAY DE BYTES DE LA FUNCION sendAndRececievDatagrams
	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;
	/**
	 * Nombre/IP del host donde se ejecuta el directorio
	 */
	private String directoryHostname;
	
	

	public static class DownloadedFile {
		public final String filename;
		public final long filesize;
		public final byte[] data;
		public final String filehash;

		public DownloadedFile(String filename, long fsize, byte[] data, String filehash) {
			this.filename = filename;
			this.filesize = fsize;
			this.data = data;
			this.filehash = filehash;
		}
	}

	public DirectoryConnector(String hostname) throws IOException {
		// Guardamos el string con el nombre/IP del host
		directoryHostname = hostname;
		/*
		 * (Boletín SocketsUDP) Convertir el string 'hostname' a InetAddress y
		 * guardar la dirección de socket (address:DIRECTORY_PORT) del directorio en el
		 * atributo directoryAddress, para poder enviar datagramas a dicho destino.
		 */
		this.directoryAddress = new InetSocketAddress(InetAddress.getByName(hostname), DIRECTORY_PORT);
		/*
		 * (Boletín SocketsUDP) Crea el socket UDP en cualquier puerto para enviar
		 * datagramas al directorio
		 */
		this.socket = new DatagramSocket();

	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	private byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];
		byte response[] = null;
		// Si no tenemos directorio, constructor previo, no podemos hacer nada, no hay servidor al que mandar los paquetes
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		// Si no tenemos socket, no podemos hacer nada
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
		/*
		 * (Boletín SocketsUDP) Enviar datos en un datagrama al directorio y
		 * recibir una respuesta. El array devuelto debe contener únicamente los datos
		 * recibidos, *NO* el búfer de recepción al completo.
		 */
		DatagramPacket datagrama = new DatagramPacket(requestData, requestData.length, directoryAddress);
		int intento = 0;
		while((intento <=MAX_NUMBER_OF_ATTEMPTS) && (response == null)) {	// BUCLE TERCER TO-do
			try {
				// Mandamos el socket
				System.out.printf("Enviamos datagrama [%d/%d]...%n", intento, MAX_NUMBER_OF_ATTEMPTS);
				this.socket.send(datagrama);
				
				// aqui debemos esperar una respuesta
				byte[] bufferRespuesta = new byte[DirMessage.PACKET_MAX_SIZE];	// LUEGO CAMBIAREMOS EL VALOR 1000 POR LA CONSTANTE DE ARRIBA
				
				System.out.println("Esperamos la respuesta del otro peer. ");
				DatagramPacket respuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
				
				System.out.println("Respuesta recibida");
				this.socket.setSoTimeout(TIMEOUT);
				socket.receive(respuesta);
				
				// ya tengo mi respuesta
				response = new byte[respuesta.getLength()];
				System.arraycopy(bufferRespuesta, 0, response, 0, response.length);
				System.out.println("Respuesta: " + new String(response));
				
				/*
				 * (Boletín SocketsUDP) Una vez el envío y recepción asumiendo un canal
				 * confiable (sin pérdidas) esté terminado y probado, debe implementarse un
				 * mecanismo de retransmisión usando temporizador, en caso de que no se reciba
				 * respuesta en el plazo de TIMEOUT. En caso de salte el timeout, se debe volver
				 * a enviar el datagrama y tratar de recibir respuestas, reintentando como
				 * máximo en MAX_NUMBER_OF_ATTEMPTS ocasiones.
				 */
				/*
				 * (Boletín SocketsUDP) Las excepciones que puedan lanzarse al
				 * leer/escribir en el socket deben ser capturadas y tratadas en este método. Si
				 * se produce una excepción de entrada/salida (error del que no es posible
				 * recuperarse), se debe informar y terminar el programa.
				 */
				/*
				 * NOTA: Las excepciones deben tratarse de la más concreta a la más genérica.
				 * SocketTimeoutException es más concreta que IOException.
				 */
				
				
			} catch(SocketTimeoutException s) {
				intento++;
				System.out.println("Timeout vencido. Reintentamos");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		if (response != null && response.length == responseData.length) {
			System.err.println("Your response is as large as the datagram reception buffer!!\n"
					+ "You must extract from the buffer only the bytes that belong to the datagram!");
		}
		return response;
	}

	/**
	 * Método para probar la comunicación con el directorio mediante el envío y
	 * recepción de mensajes sin formatear ("en crudo")
	 * 
	 * @return verdadero si se ha enviado un datagrama y recibido una respuesta
	 */
	public boolean testSendAndReceive() {
		// ESTE METODO HACE LA PRUEBA
		/*
		 * (Boletín SocketsUDP) Probar el correcto funcionamiento de
		 * sendAndReceiveDatagrams. Se debe enviar un datagrama con la cadena "ping" y
		 * comprobar que la respuesta recibida empieza por "pingok". En tal caso,
		 * devuelve verdadero, falso si la respuesta no contiene los datos esperados.
		 */
		boolean success = false;

		//"ping"
		//respuesta = "pingok"
		
		String ping = new String("ping");
		// transformo el texto en bytes, llamo a la funcion que lo manda al servidor
		byte[] respuesta = sendAndReceiveDatagrams(ping.getBytes());
		if(respuesta == null) {
			return false;
		}
		String pingRespuesta = new String(respuesta);
		
		if(pingRespuesta.equals("pingok")) {
			success = true;
		}

		return success;
	}

	public String getDirectoryHostname() {
		return directoryHostname;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que
	 * usa un protocolo compatible. Este método no usa mensajes bien formados.
	 * 
	 * @return Verdadero si
	 */
	public boolean pingDirectoryRaw() {
		boolean success = false;
		/*
		 * (Boletín EstructuraNanoFiles) Basándose en el código de
		 * "testSendAndReceive", contactar con el directorio, enviándole nuestro
		 * PROTOCOL_ID (ver clase NanoFiles). Se deben usar mensajes "en crudo" (sin un
		 * formato bien definido) para la comunicación.
		*/
		
		/*
		 * PASOS: 1.Crear el mensaje a enviar (String "ping&protocolId"). 2.Crear un
		 * datagrama con los bytes en que se codifica la cadena : 4.Enviar datagrama y
		 * recibir una respuesta (sendAndReceiveDatagrams). : 5. Comprobar si la cadena
		 * recibida en el datagrama de respuesta es "welcome", imprimir si éxito o
		 * fracaso. 6.Devolver éxito/fracaso de la operación.
		 */
		
		// EL SEND AND RECEIVE HACE PASO 2 Y 4
		
		byte[] request = new String("ping&" + NanoFiles.PROTOCOL_ID).getBytes();
		System.out.println("[pingDirectoryRaw] request; " + request);
		byte[] response = sendAndReceiveDatagrams(request);
		
		if(response == null) {
			return false;
		}
		String resp = new String(response);
		System.out.println("Contenidos recibidos del datagrama: "+ resp);
		success = resp.equals("welcome");

		return success;
	}

	/**
	 * Método para "hacer ping" al directorio, comprobar que está operativo y que es
	 * compatible.
	 * 
	 * @return Verdadero si el directorio está operativo y es compatible
	 */
	public boolean pingDirectory() {
		boolean success = false;
		/*
		 * (Boletín MensajesASCII) Hacer ping al directorio 
		 * 1.Crear el mensaje a enviar (objeto DirMessage) con atributos adecuados (operation, etc.) 
		 * NOTA:Usar como operaciones las constantes definidas en la clase DirMessageOps :
		 * 2.Convertir el objeto DirMessage a enviar a un string (método toString)
		 * 3.Crear un datagrama con los bytes en que se codifica la cadena : 
		 * 4.Enviar datagrama y recibir una respuesta (sendAndReceiveDatagrams). : 
		 * 5.Convertir respuesta recibida en un objeto DirMessage (método DirMessage.fromString)
		 * 6.Extraer datos del objeto DirMessage y procesarlos 
		 * 7.Devolver éxito/fracaso de la operación
		 */
		
		DirMessage ping = new DirMessage(DirMessageOps.OPERATION_PING);
		ping.setProtocolID(NanoFiles.PROTOCOL_ID);
		//System.out.println("[pingDirectory] protocolid: " + ping.getProtocolId().toString());
 
		byte[] bytesRespuesta = sendAndReceiveDatagrams(ping.toString().getBytes());
		
		if (bytesRespuesta == null) {
			return success;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		//System.out.println("[pingDirectory]] DEBUG: " + dmRespuesta.getOperation() + "= ping_ok"  );
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_PING_OK)) {
			System.out.println("[pingDirectory] Operación recibida: " + dmRespuesta.getOperation());
			success = true;
		}else {
			System.err.println("[pingDirectory] ERROR: ping no compatible");
		}

		return success;
	}

	/**
	 * Método para dar de alta como servidor de ficheros en el puerto indicado.
	 * 
	 * @param serverPort El puerto TCP en el que este peer sirve ficheros a otros
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y acepta la lista de ficheros, falso en caso contrario.
	 */
	public boolean registerFileServer(int serverPort) {
		//System.out.println("[registerFileServer] DEBUG: ENTRA EN FUNCION");
		boolean success = false;
		DirMessage serve = new DirMessage(DirMessageOps.OPERATION_SERVE);
		serve.setServeNombrePeer(NanoFiles.peerNickname);
		serve.setServePort(serverPort);
 
		byte[] bytesRespuesta = sendAndReceiveDatagrams(serve.toString().getBytes());
		
		if (bytesRespuesta == null) {
			return success;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		//System.out.println("[registerFileServer]] DEBUG: " + dmRespuesta.getOperation() + "= serve_ok"  );
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_SERVE_OK)) {
			System.out.println("[registerFileServer] Operación recibida: " + dmRespuesta.getOperation());
			success = true;
		}
		
		return success;
	}

	/**
	 * Método para obtener la lista de ficheros alojados en el directorio. Para cada
	 * fichero se debe obtener un objeto FileInfo con nombre, tamaño y hash.
	 * 
	 * @return Los ficheros disponibles en el directorio, o null si el directorio no
	 *         pudo satisfacer nuestra solicitud
	 */
	public FileInfo[] getFileList() {
		DirMessage dirfiles = new DirMessage(DirMessageOps.OPERATION_DIRFILES);
		byte[] datosEnvio = dirfiles.toString().getBytes();
		
		int intento = 1;
		
		while (intento <= MAX_NUMBER_OF_ATTEMPTS) {
			try {
				ArrayList<FileInfo> ficherosTotales = new ArrayList<>();
				int fragmentosEsperados = -1;
				int fragmentosRecibidos = 0;
		
				// 1. Enviamos la petición
				DatagramPacket paqueteEnvio = new DatagramPacket(datosEnvio, datosEnvio.length, directoryAddress);
				socket.send(paqueteEnvio);
	
				socket.setSoTimeout(TIMEOUT); 
	
				while (fragmentosEsperados == -1 || fragmentosRecibidos < fragmentosEsperados) {
	
					byte[] bufRcv = new byte[DirMessage.PACKET_MAX_SIZE];
					DatagramPacket paqueteRecibo = new DatagramPacket(bufRcv, bufRcv.length);

					socket.receive(paqueteRecibo); 
	
					String strRecepcion = new String(paqueteRecibo.getData(), 0, paqueteRecibo.getLength());
					DirMessage respuesta = DirMessage.fromString(strRecepcion);
	
					if (respuesta.getOperation().equals(DirMessageOps.OPERATION_DIRFILES_OK)) {
						
						if (fragmentosEsperados == -1) {
							fragmentosEsperados = respuesta.getChunksTotales();
						}
						
						FileInfo[] ficherosChunk = respuesta.getFilelist();
						if (ficherosChunk != null) {
							ficherosTotales.addAll(java.util.Arrays.asList(ficherosChunk));
						}
						
						fragmentosRecibidos++;
						System.out.println("[getFileList] Recibido fragmento " + respuesta.getChunkActual() + "/" + fragmentosEsperados);
					}
				}
				
				socket.setSoTimeout(0); // Restauramos el timeout por seguridad
				return ficherosTotales.toArray(new FileInfo[0]); // Devolvemos la lista directamente
	
			} catch (SocketTimeoutException e) {
				System.err.println("[getFileList] Error de red: Se perdió un fragmento (Timeout). Intento " + intento + "/" + MAX_NUMBER_OF_ATTEMPTS);
				intento++;
				
			} catch (IOException e) {
				System.err.println("[getFileList] Error I/O: " + e.getMessage());
				return null; // Los errores I/O sí son graves, aquí sí abortamos.
			}
		}
		
		// Si el bucle termina porque hemos superado el máximo de intentos sin éxito:
		System.err.println("[getFileList] Operación abortada tras " + MAX_NUMBER_OF_ATTEMPTS + " intentos fallidos.");
		return null;
	}
		/*
		byte[] bytesRespuesta = sendAndReceiveDatagrams(dirfiles.toString().getBytes());
		if (bytesRespuesta == null) {
			return null;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		
	//	System.out.println("[getFileList] DEBUG: " + dmRespuesta.getOperation() + "= dirfiles_ok"  );
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_DIRFILES_OK)) {
			System.out.println("[getFileList] Operación recibida: " + dmRespuesta.getOperation());
		}
		
		FileInfo[] filelist = dmRespuesta.getFilelist();
		return filelist;*/

	public Map<String, InetSocketAddress> getPeerList() {
		Map<String, InetSocketAddress> peers = new LinkedHashMap<String, InetSocketAddress>();
		
		DirMessage listaPeers = new DirMessage(DirMessageOps.OPERATION_PEERS);
		
		byte[] bytesRespuesta = sendAndReceiveDatagrams(listaPeers.toString().getBytes());

		if (bytesRespuesta == null) {
			return null;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		//System.out.println("[getPeerList] DEBUG: " + dmRespuesta.getOperation() + "= peers_ok"  );
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_PEERS_OK)) {
			System.out.println("[getPeerList] Operación recibida: " + dmRespuesta.getOperation());
		}
		
		peers = dmRespuesta.getPeers();
		return peers;
	}

	public Map<String, InetSocketAddress[]> searchFilesByHash(String hashSubstring) {
		Map<String, InetSocketAddress[]> results = new LinkedHashMap<String, InetSocketAddress[]>();



		return results;
	}

	public DownloadedFile downloadFileFromDirectory(String hashSubstring) {
		/*
		byte[] fileData = new byte[0]; 
		String filename = null;
		long filesize = -1;
		String filehash = null;

		DirMessage dirdl = new DirMessage(DirMessageOps.OPERATION_DIRDL, hashSubstring);
		byte[] bytesRespuesta = sendAndReceiveDatagrams(dirdl.toString().getBytes());
		if (bytesRespuesta == null) {
			return null;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_DIRDL_OK)) {
			System.out.println("[DownloadedFile] Operación recibida: " + dmRespuesta.getOperation());
			filename = dmRespuesta.getDirdlName();
			filehash = dmRespuesta.getDirdlhash();
			filesize = dmRespuesta.getDirdlSize();
			fileData = dmRespuesta.getDirdlData();
			
			
			//creamos un fich3ro nuevo, para opder descrgar los datos
			/*
			File descargado = new File (NanoFiles.DEFAULT_SHARED_DIRNAME,filename);
			//if (!descargado.exists()) {
				try {
					descargado.createNewFile();
					FileOutputStream fos = new FileOutputStream(descargado);
					fos.write(fileData);
					fos.close();
					System.out.print("DEBUG [DirectoryConector] descargado en path " + descargado.getAbsolutePath() );
					} catch (IOException e) {
					System.out.print("DEBUG [DirectoryConector] cathc en DridlOK");
					e.printStackTrace();
				}
				
			//}
			 * 
			 
			
			
		}else if (dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_DIRDL_ERROR)){
			return null;
		}
		*/
		byte[] fileData = new byte[0]; 
		String filename = null;
		long filesize = -1;
		String filehash = null;
		byte[] bufferdata = new byte[0];

		DirMessage dirdl = new DirMessage(DirMessageOps.OPERATION_DIRDL, hashSubstring);
		
		byte responseData[] = dirdl.toString().getBytes();
		byte response[] = null;
		// Si no tenemos directorio, constructor previo, no podemos hacer nada, no hay servidor al que mandar los paquetes
		if (directoryAddress == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP server destination address is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"directoryAddress\"");
			System.exit(-1);

		}
		// Si no tenemos socket, no podemos hacer nada
		if (socket == null) {
			System.err.println("DirectoryConnector.sendAndReceiveDatagrams: UDP socket is null!");
			System.err.println(
					"DirectoryConnector.sendAndReceiveDatagrams: make sure constructor initializes field \"socket\"");
			System.exit(-1);
		}
	
		DatagramPacket datagrama = new DatagramPacket(responseData, responseData.length, directoryAddress);
		
		
		int chunktotales = 0;
		int chunckactual = -1;
		long posicion=0;
		int intento = 1;
		while(intento <=MAX_NUMBER_OF_ATTEMPTS) {	// BUCLE TERCER TO-do
			try {
				// Mandamos el socket
				System.out.printf("Enviamos datagrama [%d/%d]...%n", intento, MAX_NUMBER_OF_ATTEMPTS);
				this.socket.send(datagrama);
				
				// aqui debemos esperar una respuesta
				//byte[] bufferRespuesta = new byte[DirMessage.PACKET_MAX_SIZE];	// LUEGO CAMBIAREMOS EL VALOR 1000 POR LA CONSTANTE DE ARRIBA
				
				byte[] bufferRespuesta = new byte[DirMessage.PACKET_MAX_SIZE];	// LUEGO CAMBIAREMOS EL VALOR 1000 POR LA CONSTANTE DE ARRIBA
				while(chunckactual<chunktotales || chunckactual == -1) {
				
					System.out.println("Esperamos la respuesta del otro peer. ");
					DatagramPacket paqueterespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
					
					System.out.println("Respuesta recibida");
					this.socket.setSoTimeout(TIMEOUT);
					socket.receive(paqueterespuesta);
					
					
					String strrespuesta = new String(paqueterespuesta.getData(), 0, paqueterespuesta.getLength());
					DirMessage respuesta = DirMessage.fromString(strrespuesta);
				
					chunckactual =respuesta.getChunkActual();
					chunktotales = respuesta.getChunksTotales();
					System.out.println("DEBUG: Chunks: " + chunckactual + " / " + chunktotales);
					System.out.println("DEBUG: OP: " + respuesta.getOperation()) ;
					
					if (respuesta.getOperation().equals(DirMessageOps.OPERATION_DIRDL_OK))  {
						filename = respuesta.getDirdlName();
						filehash = respuesta.getDirdlhash();
						filesize = respuesta.getDirdlSize();
					
						System.out.println("DEBUG: Posicion: " + respuesta.getPosicion()) ;
						posicion =  respuesta.getPosicion();
						
						bufferdata = Arrays.copyOf(bufferdata,(int) filesize);
						
						System.arraycopy(respuesta.getDirdlData(), 0, bufferdata, (int)posicion, respuesta.getDirdlData().length);
						
						System.out.println("DEBUG: data: " + bufferdata + " tamaño: " + bufferdata.length);
						
						
						
					}else if (respuesta.getOperation().equals(DirMessageOps.OPERATION_DIRDL_ERROR)){
						return null;
					}
					
					//chunckactual = chunckactual + bufferdata.length; //esto es lo que llevamos del chunk
					System.out.println("DEBUG: Tamañodata: " + bufferdata.length);
					System.out.println("DEBUG: Chunk: " + chunckactual + " / " + chunktotales);
	
					System.out.println("DEBUG: datoschunck1,  name : " + filename  +
							"\n size: " + filesize + "\n hash: " + filehash + "\n datazize: " +  bufferdata.length);
				}
				fileData =  bufferdata;
				
				
				
				System.out.println("DEBUG: datos downloadfile0,  name : " + filename  +
						"\n size: " + filesize + "\n hash: " + filehash + "\n datazize: " +  fileData.length);
						
				
				socket.setSoTimeout(0); // Restauramos el timeout por seguridad
			
				return new DownloadedFile(filename, filesize, fileData, filehash);
				
			} catch(SocketTimeoutException s) {
				intento++;
				System.out.println("Timeout vencido. Reintentamos");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


			
		return null;
	}

	/**
	 * Método para darse de baja como servidor de ficheros. 
	 * 
	 * @return Verdadero si el directorio tiene registrado a este peer como servidor
	 *         y ha dado de baja sus ficheros.
	 */
	// ESTE ES EL QUIT
	public boolean unregisterFileServer() {
		boolean success = false;

		DirMessage msg = new DirMessage(DirMessageOps.OPERATION_QUIT);
		msg.setServeNombrePeer(NanoFiles.peerNickname);

		byte[] bytesRespuesta = sendAndReceiveDatagrams(msg.toString().getBytes());
		
		if (bytesRespuesta == null) {
			return success;
		}
		
		String stringRespuesta = new String(bytesRespuesta, 0 , bytesRespuesta.length);
		DirMessage dmRespuesta = DirMessage.fromString(stringRespuesta);
		
		if(dmRespuesta.getOperation().equals(DirMessageOps.OPERATION_QUIT_OK)) {
			System.out.println("[unregisterFileServer] Operación recibida: " + dmRespuesta.getOperation());
			success = true;
		}
		return success;
	}
}
