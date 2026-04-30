package es.um.redes.nanoFiles.udp.server;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.udp.message.DirMessage;
import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFDirectoryServer {
	/**
	 * Número de puerto UDP en el que escucha el directorio
	 */
	public static final int DIRECTORY_PORT = 6868;

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	private DatagramSocket socket = null;
	/*
	 * Añadir aquí como atributos las estructuras de datos que sean necesarias
	 * para mantener en el directorio cualquier información necesaria para la
	 * funcionalidad del sistema nanoFilesP2P: ficheros alojados, servidores
	 * registrados, etc.
	 */
	/**
	 * Lista de ficheros alojados en el directorio.
	 */
	private FileInfo[] directoryFiles;
	/**
	 * Lista de servidores registrados (IP, puerto TCP).
	 */
	private LinkedHashMap<String, InetSocketAddress> registeredPeers;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	private double messageDiscardProbability;

	public NFDirectoryServer(double corruptionProbability, String directoryFilesPath) throws SocketException {
		/*
		 * Guardar la probabilidad de pérdida de datagramas (simular enlace no
		 * confiable)
		 */
		messageDiscardProbability = corruptionProbability;
		/*
		 * Cargar los ficheros del directorio compartido.
		 */
		File dir = new File(directoryFilesPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		directoryFiles = FileInfo.loadFilesFromFolder(directoryFilesPath);
		System.out.println("* Directory loaded " + directoryFiles.length + " files from " + directoryFilesPath);
		/*
		 * (Boletín SocketsUDP) Inicializar el atributo socket: Crear un socket
		 * UDP ligado al puerto especificado por el argumento directoryPort en la
		 * máquina local,
		 */
		this.socket = new DatagramSocket(DIRECTORY_PORT);
		/*
		 * (Boletín SocketsUDP) Inicializar atributos que mantienen el estado del
		 * servidor de directorio: peers registrados, etc.)
		 */
		this.registeredPeers = new LinkedHashMap<String, InetSocketAddress>();


		if (NanoFiles.testModeUDP) {
			if (socket == null) {
				System.err.println("[testMode] NFDirectoryServer: code not yet fully functional.\n"
						+ "Check that all TODOs in its constructor and 'run' methods have been correctly addressed!");
				System.exit(-1);
			}
		}
	}

	public DatagramPacket receiveDatagram() throws IOException {
		DatagramPacket datagramReceivedFromClient = null;
		boolean datagramReceived = false;
		while (!datagramReceived) {
			/*
			 * (Boletín SocketsUDP) Crear un búfer para recibir datagramas y un
			 * datagrama asociado al búfer (datagramReceivedFromClient)
			 */
			byte[] buf = new byte[DirMessage.PACKET_MAX_SIZE];
			datagramReceivedFromClient = new DatagramPacket(buf, buf.length);
			/*
			 * (Boletín SocketsUDP) Recibimos a través del socket un datagrama
			 */
			this.socket.receive(datagramReceivedFromClient);

			/*
			if (datagramReceivedFromClient == null) {
				System.err.println("[testMode] NFDirectoryServer.receiveDatagram: code not yet fully functional.\n"
						+ "Check that all TODOs have been correctly addressed!");
				System.exit(-1);
			} else {*/
				// Vemos si el mensaje debe ser ignorado (simulación de un canal no confiable)
				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println(
							"Directory ignored datagram from " + datagramReceivedFromClient.getSocketAddress());
				} else {
					datagramReceived = true;
					System.out
							.println("Directory received datagram from " + datagramReceivedFromClient.getSocketAddress()
									+ " of size " + datagramReceivedFromClient.getLength() + " bytes.");
				}
			//}
		}
		return datagramReceivedFromClient;
	}

	public void runTest() throws IOException {

		System.out.println("[testMode] Directory starting...");

		System.out.println("[testMode] Attempting to receive 'ping' message...");
		DatagramPacket rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);

		System.out.println("[testMode] Attempting to receive 'ping&PROTOCOL_ID' message...");
		rcvDatagram = receiveDatagram();
		sendResponseTestMode(rcvDatagram);
	}

	private void sendResponseTestMode(DatagramPacket pkt) throws IOException {
		/*
		 * (Boletín SocketsUDP) Construir un String partir de los datos recibidos
		 * en el datagrama pkt. A continuación, imprimir por pantalla dicha cadena a
		 * modo de depuración.
		 */
		String response = new String(pkt.getData(), 0, pkt.getLength()); // pkt.getOffset(), pkt.getLength());
		System.out.println("[sendResponseTestMode] Cadena de respuesta e partir de los datos del datagrama recibido: " + response);
		/*
		 * (Boletín SocketsUDP) Después, usar la cadena para comprobar que su
		 * valor es "ping"; en ese caso, enviar como respuesta un datagrama con la
		 * cadena "pingok". Si el mensaje recibido no es "ping", se informa del error y
		 * se envía "invalid" como respuesta.
		 */
		
		if(response.equalsIgnoreCase("ping")) {
			response = new String("pingok");
			System.out.println("[sendResponseTestMode] Cadena de respuesta " + response);
		}else {
			/*
			 * (Boletín Estructura-NanoFiles) Ampliar el código para que, en el caso
			 * de que la cadena recibida no sea exactamente "ping", comprobar si comienza
			 * por "ping&" (es del tipo "ping&PROTOCOL_ID", donde PROTOCOL_ID será el
			 * identificador del protocolo diseñado por el grupo de prácticas (ver
			 * NanoFiles.PROTOCOL_ID). Se debe extraer el "protocol_id" de la cadena
			 * recibida y comprobar que su valor coincide con el de NanoFiles.PROTOCOL_ID,
			 * en cuyo caso se responderá con "welcome" (en otro caso, "denied").
			 */
			
			if(response.startsWith("ping&")) {
				String codigo = (response.split("&"))[1];
				if(codigo.equals((NanoFiles.PROTOCOL_ID))){
					response = new String("welcome");
				}else {
					response = new String("denied");
				}
			}else {
				System.out.println("[senResponseTestMode] ERROR: la respuesta " + response + " no es igual a ping");
				response = new String("invalid");
			}
		}
		
		// Convertimos mensaje a bytes
		byte[] responseBytes = response.getBytes();
		// Creamos paquete de respuesta
		// pkt.getAddres y getPort, es la IP y puertos de la direccion a donde lo quiero enviar
		DatagramPacket pktResponse = new DatagramPacket(responseBytes, responseBytes.length, (InetSocketAddress) pkt.getSocketAddress());// pkt.getAddress(), pkt.getPort());
		this.socket.send(pktResponse);
		//System.out.println("[sendResponseTestMode] DEBUG: Respuesta enviada: " + response);

		String messageFromClient = new String(pkt.getData(), 0, pkt.getLength());
		System.out.println("Data received: " + messageFromClient);

	}

	public void run() throws IOException {

		System.out.println("Directory starting...");

		while (true) { // Bucle principal del servidor de directorio
			DatagramPacket rcvDatagram = receiveDatagram();

			sendResponse(rcvDatagram);

		}
	}

	private void sendResponse(DatagramPacket pkt) throws IOException {
		/*
		 * (Boletín MensajesASCII) Construir String partir de los datos recibidos
		 * en el datagrama pkt. A continuación, imprimir por pantalla dicha cadena a
		 * modo de depuración. Después, usar la cadena para construir un objeto
		 * DirMessage que contenga en sus atributos los valores del mensaje. A partir de
		 * este objeto, se podrá obtener los valores de los campos del mensaje mediante
		 * métodos "getter" para procesar el mensaje y consultar/modificar el estado del
		 * servidor.
		 */
		String mensaje = new String(pkt.getData(), 0, pkt.getLength());
		//System.out.println("[sendResponse] DEBUG: Cadena recibida a partir de los datos del datagramPacket: " + mensaje);

		DirMessage mensajeCliente = DirMessage.fromString(mensaje);
		//System.out.println("[sendResponse] DEBUG: Mensaje cliente: " + mensajeCliente.toString());
		/*
		 * Una vez construido un objeto DirMessage con el contenido del datagrama
		 * recibido, obtener el tipo de operación solicitada por el mensaje y actuar en
		 * consecuencia, enviando uno u otro tipo de mensaje en respuesta.
		 */
		String operation = mensajeCliente.getOperation();

		/*
		 * (Boletín MensajesASCII) Construir un objeto DirMessage (msgToSend) con
		 * la respuesta a enviar al cliente, en función del tipo de mensaje recibido,
		 * leyendo/modificando según sea necesario el "estado" guardado en el servidor
		 * de directorio (atributos files, etc.). Los atributos del objeto DirMessage
		 * contendrán los valores adecuados para los diferentes campos del mensaje a
		 * enviar como respuesta (operation, etc.)
		 */

		DirMessage mensajeAEnviar = null;
		//System.out.println("[sendResponse] DEBUG: Operation: " + operation);
		switch (operation) {
		case DirMessageOps.OPERATION_PING: {

			/*
			 * (Boletín MensajesASCII) Comprobamos si el protocolId del mensaje del
			 * cliente coincide con el nuestro.
			 */
			//System.out.println("[sendResponse] DEBUG: " + mensajeCliente.getProtocolId()+" = "  + NanoFiles.PROTOCOL_ID);
			if(mensajeCliente.getProtocolId().equals(NanoFiles.PROTOCOL_ID)) {
				/*
				 * (Boletín MensajesASCII) Construimos un mensaje de respuesta que indique
				 * el éxito/fracaso del ping (compatible, incompatible), y lo devolvemos como
				 * resultado del método.
				 */
				/*
				 * (Boletín MensajesASCII) Imprimimos por pantalla el resultado de
				 * procesar la petición recibida (éxito o fracaso) con los datos relevantes, a
				 * modo de depuración en el servidor
				 */
				
				mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_PING_OK);
				System.out.println("[sendResponse] Ping recibido. Protocolo compatible");
			}
			else {
				mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_PING_ERROR);
				System.err.println("[sendResponse] Ping recibido. Protocolo incompatible");
			}
			break;

		}case DirMessageOps.OPERATION_DIRFILES: {
			int MAX_FICHEROS_CHUNK = 10;	// PONER 10/15
			int totalFicheros = directoryFiles.length;

			if(totalFicheros == 0) {
				System.out.println("[sendResponse] File recibido. File dir-shared vacio, no hay ficheros a imprimir");
				mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_DIRFILES_OK, directoryFiles);
				break;
			}
			else {
				System.out.println("[sendResponse] File recibido. File dir-shared no vacio, hay ficheros a imprimir");
				// calcular chunks totales
				int chunksTotales = (totalFicheros + MAX_FICHEROS_CHUNK - 1) / MAX_FICHEROS_CHUNK;
				
				// Bucle para crear y enviar cada chunk
				for (int i = 1; i <= chunksTotales; i++) {
					int inicio = (i - 1) * MAX_FICHEROS_CHUNK;
					int fin = Math.min(inicio + MAX_FICHEROS_CHUNK, totalFicheros);

					// Copiamos el trozo del array
					FileInfo[] ficherosFragmento = java.util.Arrays.copyOfRange(directoryFiles, inicio, fin);

					// Creamos el mensaje de respuesta
					mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_DIRFILES_OK, ficherosFragmento);
					mensajeAEnviar.setChunkActual(i);
					mensajeAEnviar.setChunksTotales(chunksTotales);
					
					// Enviamos este fragmento inmediatamente
					byte[] bytesPayload = mensajeAEnviar.toString().getBytes();
					this.socket.send(new DatagramPacket(bytesPayload, bytesPayload.length, pkt.getSocketAddress()));
					
					System.out.println("[sendResponse] Enviado fragmento " + i + "/" + chunksTotales);
				}
				return;
			}
		}case DirMessageOps.OPERATION_PEERS: {
			mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_PEERS_OK, registeredPeers);
			break;
		}case DirMessageOps.OPERATION_SERVE: {
			String cadIp = pkt.getAddress().getHostAddress();
			
			int puerto =  mensajeCliente.getServePort();
			String nombrePeer = mensajeCliente.getServeNombrePeer();
			InetSocketAddress pareja =  new InetSocketAddress(InetAddress.getByName(cadIp), puerto);
			mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_SERVE_OK, nombrePeer, cadIp, puerto);
			
			if(!registeredPeers.containsKey(nombrePeer)) {
				registeredPeers.put(nombrePeer, pareja);	
			}
			
			break;
		}
		// AMPLIACIÓN
		case DirMessageOps.OPERATION_DIRDL: {
			String subhash = mensajeCliente.getDirdlHashSubstring();
			String name = null;
			String hash = null;
			String path = null;
			Long size = (long) 0;
			byte[] data = new byte[0];
			
			for (FileInfo f: directoryFiles) {
				if (f.fileHash.contains(subhash)) {
					hash = f.fileHash;
					name = f.fileName;
					size = f.fileSize;
					path = f.filePath;
					break;
				}
			}

			if (name == null) {
				mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_DIRDL_ERROR);

			}else {
				File fichero = new File (path);
				DataInputStream dis = new DataInputStream(new FileInputStream(fichero));
				long filelength = fichero.length();
				data = new byte[(int) filelength]; //data ya tiene todo el contenido del fichero
				dis.readFully(data);
				dis.close();
				System.out.println("[sendResponse] DEBUG: Enviando datos con longitud " + data.length);
				mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_DIRDL_OK, hash, name, size, data);
			}
			break;
		}case DirMessageOps.OPERATION_QUIT: {			
			String nombrePeer = mensajeCliente.getServeNombrePeer();
			String ipPeticion = pkt.getAddress().getHostAddress();
						
			if(registeredPeers.containsKey(nombrePeer)) {
				InetSocketAddress datosGuardados = registeredPeers.get(nombrePeer);
				// el peer y la ip coinciden y ademas esta en la lista de peers registrados
				if (datosGuardados.getAddress().getHostAddress().equals(ipPeticion)) {
		            registeredPeers.remove(nombrePeer);
		            mensajeAEnviar = new DirMessage(DirMessageOps.OPERATION_QUIT_OK);
		        }
			}
			break;
		}
		default:
			System.err.println("[sendResponse] Unexpected message operation: \"" + operation + "\"");
			System.exit(-1);
		}

		/*
		 * (Boletín MensajesASCII) Convertir a String el objeto DirMessage
		 * (msgToSend) con el mensaje de respuesta a enviar, extraer los bytes en que se
		 * codifica el string y finalmente enviarlos en un datagrama
		 */
		
		byte[] bytesPayload = mensajeAEnviar.toString().getBytes();
		this.socket.send(new DatagramPacket(bytesPayload, bytesPayload.length, (InetSocketAddress)pkt.getSocketAddress()));
	}
}
