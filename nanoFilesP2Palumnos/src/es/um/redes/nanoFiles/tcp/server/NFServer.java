package es.um.redes.nanoFiles.tcp.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

import es.um.redes.nanoFiles.application.NanoFiles;
import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;


public class NFServer implements Runnable {

	public static final int PORT = 10000;
	private ServerSocket serverSocket = null;
	
	// variables para los metodos de más adelante
	private boolean isRunning = false;
	private Thread serverThread;

	public NFServer() throws IOException {
		/*
		 * (Boletín SocketsTCP) Crear una direción de socket a partir del puerto
		 * especificado (PORT)
		 */
		InetSocketAddress dirSocket = new InetSocketAddress(PORT);
		/*
		 * (Boletín SocketsTCP) Crear un socket servidor y ligarlo a la dirección
		 * de socket anterior
		 */
		serverSocket = new ServerSocket();
		serverSocket.bind(dirSocket);
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación.
	 * 
	 */
	public void test() {
		if (serverSocket == null || !serverSocket.isBound()) {
			System.err.println(
					"[fileServerTestMode] Failed to run file server, server socket is null or not bound to any port");
			return;
		} else {
			System.out.println("[fileServerTestMode] NFServer running on " + serverSocket.getLocalSocketAddress() + ".");
		}

		//utilizamos el serversocket
		while (true) {
			//System.out.println("[fileServerTestMode] DEBUG entra en el true");
			/*
			 * (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
			 * otros peers que soliciten descargar ficheros.
			 */
			try {
				//System.out.println("[fileServerTestMode] DEBUG entra en el try");
				Socket clientSocket = this.serverSocket.accept();
				System.out.println("\nNew client connected: " +
						clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());
				/*
				 * (Boletín SocketsTCP) Tras aceptar la conexión con un peer cliente, la
				 * comunicación con dicho cliente para servir los ficheros solicitados se debe
				 * implementar en el método serveFilesToClient, al cual hay que pasarle el
				 * socket devuelto por accept.
				 */
				serveFilesToClient(clientSocket);
				
			} catch (IOException e) {
				//System.out.println("[fileServerTestMode] DEBUG entra en el cath");
				e.printStackTrace();
			}
		}
	}

	
	
	public boolean isRunning() {
		return isRunning;
	}

	/**
	 * Método que ejecuta el hilo principal del servidor en segundo plano, esperando
	 * conexiones de clientes.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		/*
		 * (Boletín SocketsTCP) Usar el socket servidor para esperar conexiones de
		 * otros peers que soliciten descargar ficheros
		 */	
		Socket clientSocket;
		try {
			while(isRunning) {
				clientSocket = this.serverSocket.accept();
				System.out.println("\n[NFServer]New client connected: " +
						clientSocket.getInetAddress().toString() + ":" + clientSocket.getPort());
				
				/*
				 * (Boletín SocketsTCP) Al establecerse la conexión con un peer, la
				 * comunicación con dicho cliente se hace en el método
				 * serveFilesToClient(socket), al cual hay que pasarle el socket devuelto por
				 * accept
				 */
				
				//serveFilesToClient(clientSocket); LO QUITAMOS PARA QUE EL SERVIDOR NO SE QUEDE ATASCADO
				/*
				 * (Boletín TCPConcurrente) Crear un hilo nuevo de la clase
				 * NFServerThread, que llevará a cabo la comunicación con el cliente que se
				 * acaba de conectar, mientras este hilo vuelve a quedar a la escucha de
				 * conexiones de nuevos clientes (para soportar múltiples clientes). Si este
				 * hilo es el que se encarga de atender al cliente conectado, no podremos tener
				 * más de un cliente conectado a este servidor.
				 */
				
				NFServerThread clientThread = new NFServerThread(clientSocket);
				clientThread.start();
				
			} 
		}catch (java.net.SocketException e) {
	        // Capturamos el cierre del socket sin imprimir el error gigante
	        System.out.println("[NFServer] Server socket cerrado correctamente.");
	    }
		catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
		}
		
	}
	/*
	 * (Boletín SocketsTCP) Añadir métodos a esta clase para: 1) Arrancar el
	 * servidor en un hilo nuevo que se ejecutará en segundo plano 2) Detener el
	 * servidor (stopserver) 3) Obtener el puerto de escucha del servidor etc.
	 */

	public void startServer() {
		if (!isRunning) {
			isRunning = true;

			serverThread = new Thread(this, "NFServer-MainThread");
			serverThread.start();
			System.out.println("[NFServer] Server nuevo en segundo plano.");
		} else {
			System.out.println("[NFServer] Server is already running.");
		}
	}

	public void stopServer() {
		isRunning = false;
		try {
			// Es vital cerrar el ServerSocket. 
			// Esto provoca una SocketException en el método accept() que esté bloqueado,
			// permitiendo que el hilo del servidor salga de su letargo y termine.
			if (this.serverSocket != null && !this.serverSocket.isClosed()) {
				this.serverSocket.close();
				System.out.println("[NFServer] Server stopped.");
			}
		} catch (IOException e) {
			System.err.println("[NFServer] Error stopping server: " + e.getMessage());
		}
	}

	public int getServerPort() {
		if (this.serverSocket != null) {
			return this.serverSocket.getLocalPort();
		}
		return -1; // Retorna -1 si el socket no está creado
	}


	/**
	 * Método de clase que implementa el extremo del servidor del protocolo de
	 * transferencia de ficheros entre pares.
	 * 
	 * @param socket El socket para la comunicación con un cliente que desea
	 *               descargar ficheros.
	 */
	public static void serveFilesToClient(Socket socket) {
		/*
		 * (Boletín SocketsTCP) Crear dis/dos a partir del socket
		 */
		//System.out.println("[NFServer] serveFilesToClient");
		try {
			DataInputStream dis =  new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		
			/*
			 * (Boletín SocketsTCP) Mientras el cliente esté conectado, leer mensajes
			 * de socket, convertirlo a un objeto PeerMessage y luego actuar en función del
			 * tipo de mensaje recibido, enviando los correspondientes mensajes de
			 * respuesta.
			 */
			boolean clientConnected = true;
			
			// Mientras el cliente esté conectado
			while (clientConnected) {
				try {
					// Leer mensaje del cliente
					PeerMessage msgFromClient = PeerMessage.readMessageFromInputStream(dis);
					System.out.println("[serveFilesToClient] Opcode Recibido: " + msgFromClient.getOpcode());
					System.out.println(msgFromClient.toDebugString());
					switch (msgFromClient.getOpcode()) {

					/*
					 * (Boletín SocketsTCP) Para servir un fichero, hay que localizarlo a
					 * partir de su hash (o subcadena) en nuestra base de datos de ficheros
					 * compartidos. Los ficheros compartidos se pueden obtener con
					 * NanoFiles.db.getFiles(). Los métodos lookupHashSubstring y
					 * lookupFilenameSubstring de la clase FileInfo son útiles para buscar ficheros
					 * coincidentes con una subcadena dada del hash o del nombre del fichero. El
					 * método lookupFilePath() de FileDatabase devuelve la ruta al fichero a partir
					 * de su hash completo.
					 */
                    //TODO: ARREGLAR NICKNAME
	                    case PeerMessageOps.OPCODE_PEER_DL: // El cliente quiere descargar algo
	                        String subhash = msgFromClient.getPeerfileSubhash();
	                        System.out.println("[serveFilesToClient] Client requests file with subhash: " + subhash);
	
	                        
	                        FileInfo[] sharedFiles = NanoFiles.db.getFiles(); 
	                        FileInfo[] matches = FileInfo.lookupHashSubstring(sharedFiles, subhash);
	
	                        if (matches.length == 1) {
	                            // Solo hay uno que coincide
	                            String path = matches[0].filePath;
	                            
	                            // Leer los bytes del fichero real
	                            byte[] fileBytes = Files.readAllBytes(new File(path).toPath());
	
	                            // Crear mensaje de respuesta OK
	                            PeerMessage response = new PeerMessage(PeerMessageOps.OPCODE_PEER_DL_OK);
	                            
	                            response.setPeerfileData(fileBytes);
	                            response.writeMessageToOutputStream(dos);
	                            System.out.println("[serveFilesToClient] Archivo enviado con éxito (" + fileBytes.length + " bytes)");
	                            
	                        } else if (matches.length == 0) {
	                            // ERROR: no tenemos el fichero
	                            PeerMessage response = new PeerMessage(PeerMessageOps.OPCODE_PEER_DL_ERROR_CONCORDANCIA);
	                            response.writeMessageToOutputStream(dos);
	                        } else {
	                            // ERROR: Hay varios que coinciden, ambiguedad
	                            PeerMessage response = new PeerMessage(PeerMessageOps.OPCODE_PEER_DL_ERROR_AMBIGUEDAD);
	                            response.writeMessageToOutputStream(dos);
	                        }
	                        break;
	
	                    case PeerMessageOps.OPCODE_PEER_FILES: // El cliente pide nuestra lista de ficheros
	                        FileInfo[] myFiles = NanoFiles.db.getFiles();
	                        PeerMessage listResponse = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILES_OK, myFiles);
	                        listResponse.writeMessageToOutputStream(dos);
	                        break;
	
	                    default:
	                        System.out.println("[serveFilesToClient] Opcode no sirve " + msgFromClient.getOpcode());
                }
					

				} catch (EOFException e) {
					// Esta excepción salta cuando el cliente corta la conexión abruptamente
					System.out.println("[serveFilesToClient] El cliente ha cerrado la conexión.");
					clientConnected = false;
				} 			
			}
		
		} catch (IOException e) {	
			e.printStackTrace();
		}finally {
	        try {
	            socket.close();
	        } catch (IOException e) { e.printStackTrace(); }
	    }
	}

}
