package es.um.redes.nanoFiles.logic;

import java.net.InetSocketAddress;
import java.util.Map;
import java.io.IOException;
import es.um.redes.nanoFiles.tcp.client.NFConnector;
import es.um.redes.nanoFiles.application.NanoFiles;



import es.um.redes.nanoFiles.tcp.server.NFServer;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFControllerLogicP2P {
	// Servidor TCP local para compartir ficheros con otros peers
	private NFServer fileServer = null;
	private String nickp2p = "unknown";

	public void setNickp2p(String nick) {
		this.nickp2p = nick;
	}
	protected NFControllerLogicP2P() {
		this.nickp2p = "unknown";
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor en un nuevo hilo creado a tal efecto.
	 * 
	 * @return Verdadero si se ha arrancado en un nuevo hilo con el servidor de
	 *         ficheros, y está a la escucha en un puerto, falso en caso contrario.
	 * 
	 */
	protected boolean startFileServer() {
		boolean serverRunning = false;
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		if (fileServer != null && fileServer.isRunning()) {
			System.err.println("File server is already running");
			serverRunning = true;
		} else {
			/*
			 * (Boletín Servidor TCP concurrente) Arrancar servidor en segundo plano
			 * creando un nuevo hilo, comprobar que el servidor está escuchando en un puerto
			 * válido (>0), imprimir mensaje informando sobre el puerto de escucha, y
			 * devolver verdadero. Las excepciones que puedan lanzarse deben ser capturadas
			 * y tratadas en este método. Si se produce una excepción de entrada/salida
			 * (error del que no es posible recuperarse), se debe informar sin abortar el
			 * programa
			 * 
			 */
			try {
				fileServer = new NFServer();
				fileServer.startServer();
				int port = fileServer.getServerPort();
				if(port > 0) {
					System.out.println("[startFileServer] Servidor del fichero corriendo en puerto: " + port);
					serverRunning = true;
				} else {
					System.err.println("[startFileServer] Error: Servidor del fichero con puerto inválido (" + port + ")");
					fileServer = null; // Limpiamos la referencia si falló
				}
			} catch (IOException e) {
				System.err.println("[startFileServer] No se pudo iniciar el servidor: " + e.getMessage());
				fileServer = null; 
				serverRunning = false;
			}
		}
		return serverRunning;

	}

	protected void testTCPServer() {
		assert (NanoFiles.testModeTCP);
		/*
		 * Comprobar que no existe ya un objeto NFServer previamente creado, en cuyo
		 * caso el servidor ya está en marcha.
		 */
		assert (fileServer == null);
		try {
			System.out.println("DEBUG: entra en el try testTCPServer");

			fileServer = new NFServer();
			/*
			 * (Boletín SocketsTCP) Inicialmente, se creará un NFServer y se ejecutará su
			 * método "test" (servidor minimalista en primer plano, que sólo puede atender a
			 * un cliente conectado). Posteriormente, se desactivará "testModeTCP" para
			 * implementar un servidor en segundo plano, que se ejecute en un hilo
			 * secundario para permitir que este hilo (principal) siga procesando comandos
			 * introducidos mediante el shell.
			 */
			fileServer.test();
			// Este código es inalcanzable: el método 'test' nunca retorna...
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("Cannot start the file server");
			fileServer = null;
		}
	}

	public void testTCPClient() {
		System.out.println("DEBUG: entra en testTCPClient");
		assert (NanoFiles.testModeTCP);
		/*
		 * (Boletín SocketsTCP) Inicialmente, se creará un NFConnector (cliente TCP)
		 * para conectarse a un servidor que esté escuchando en la misma máquina y un
		 * puerto fijo. Después, se ejecutará el método "test" para comprobar la
		 * comunicación mediante el socket TCP. Posteriormente, se desactivará
		 * "testModeTCP" para implementar la descarga de un fichero desde múltiples
		 * servidores.
		 */

		try {
			System.out.println("DEBUG: entra en el try testTCPClient");
			NFConnector nfConnector = new NFConnector(new InetSocketAddress(NFServer.PORT), "nada");
			nfConnector.test();
			//NanoFiles.testModeTCP = false;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método para listar los ficheros de un peer concreto vía TCP e imprimirlos por
	 * pantalla.
	 * 
	 * @param La dirección del peer cuyos ficheros se quiere listar
	 * @return Verdadero si se ha obtenido exitosamente el listado de fichero del
	 *         peer
	 */
	// COMANDO PEERFILES
	protected boolean listPeerFiles(InetSocketAddress peerAddr) {
		boolean success = false;
		//System.out.println("[listPeerFiles] DEBUG: Entrando en listPeerFiles");
		
		try {
			//System.out.println("[listPeerFiles] DEBUG: entra en try");
			NFConnector peerConnector = new NFConnector(peerAddr, nickp2p);
			//System.out.println("[listPeerFiles] DEBUG: PROBANDO PEERFILES nickp2p = " + nickp2p);
			//System.out.println("[listPeerFiles] DEBUG: conexión establecida");
			peerConnector.setNickname(nickp2p);
			FileInfo[] ficherosDelPeer = peerConnector.getFileList();
			
			if (ficherosDelPeer != null) {
				System.out.println("* Files shared by peer:");
				FileInfo.printToSysout(ficherosDelPeer);
				success = true;
			} else {
				//System.out.println("[listPeerFiles] DEBUG: lista de ficheros nula");
				System.err.println("* El peer no ha devuelto ningún fichero o hubo un error.");
			}
			
			peerConnector.close();
			
		} catch (IOException e) {
			System.err.println("* Error al conectar por TCP con el peer en: " + peerAddr);
			e.printStackTrace();
		}
		
		return success;
	}

	/**
	 * Descarga un fichero identificado por subcadena de hash desde uno o varios
	 * peers. Si se pasa "*" como nickname, usa el directorio para localizar los
	 * peers que tienen el hash.
	 */
	// ESTA FUNCION ES PARA EL PEERDL
	protected boolean downloadFromPeers(NFControllerLogicDir dirLogic, String targetPeerNickname, String targetHashSubstring) {
		// Localizar peers con el hash solicitado (o uno concreto) y delegar en
		// downloadFileFromServers
		boolean success = false;
		InetSocketAddress[] serverAddressList = null;
		Map<String, InetSocketAddress> peerList = dirLogic.fetchPeerList();


		if (targetPeerNickname.equals("*")) {
			System.out.println("* Buscando peers con el fichero en el directorio...");
			serverAddressList = peerList.values().toArray(new InetSocketAddress[0]);
		} else {
			/*
			 * CASO B: El usuario quiere un peer específico.
			 * Usamos el nickname para buscar su dirección en el mapa que nos dio fetchPeerList.
			 */
			if (peerList.containsKey(targetPeerNickname)) {
				InetSocketAddress addr = peerList.get(targetPeerNickname);
				serverAddressList = new InetSocketAddress[] { addr };
				System.out.println("[P2P] Peer " + targetPeerNickname + " found at " + addr);
			} else {
				System.err.println("* Error: Peer nickname '" + targetPeerNickname + "' is not registered in the directory.");
				return false;
			}
		}
		this.setNickp2p(targetPeerNickname);
		if (serverAddressList != null && serverAddressList.length > 0) {
			success = downloadFileFromServers(serverAddressList, targetHashSubstring);
		} else {
			System.err.println("* No available peers found for the requested file.");
		}
		return success;
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros
	 * 
	 * @param serverAddressList   La lista de direcciones de los servidores a los
	 *                            que se conectará
	 * @param targetHashSubstring Subcadena del hash del fichero a descargar
	 */
	// COMANDO PEERDL
	protected boolean downloadFileFromServers(InetSocketAddress[] serverAddressList, String targetHashSubstring) {
		boolean downloaded = false;

		if (serverAddressList.length == 0) {
			System.err.println("* Cannot start download - No list of server addresses provided");
			return false;
		}
		for (InetSocketAddress addr : serverAddressList) {
	        try {
	            NFConnector peerConnector = new NFConnector(addr, nickp2p);
				peerConnector.setNickname(nickp2p);

	            // Pedir el fichero usando el subhash 
	            byte[] fileData = peerConnector.downloadFile(targetHashSubstring);
	            
	            if (fileData != null) {
	                // Guardar los bytes en un fichero local
	                String localFileName = NanoFiles.sharedDirname + java.io.File.separator  + "downloaded_" + targetHashSubstring.substring(0,5);
	    			java.nio.file.Path dest = es.um.redes.nanoFiles.util.FileNameUtil.chooseAvailableName(localFileName);
	                java.nio.file.Files.write(dest, fileData);
	                
	                /*File descargado = new File (NanoFiles.DEFAULT_SHARED_DIRNAME,localFileName);
	                descargado.createNewFile();
	                FileOutputStream fos = new FileOutputStream(descargado);
	                fos.write(fileData);
	                fos.close();*/
	                
	                System.out.println("* Fichero descargado correctamente como: " + dest);
	                downloaded = true;
	                peerConnector.close();
	                break; 
	            }
	            peerConnector.close();
	        } catch (IOException e) {
	            System.err.println("* Error descargando de " + addr + ": " + e.getMessage());
	        }
	    }
		return downloaded;
	}

	/**
	 * Método para obtener el puerto de escucha de nuestro servidor de ficheros
	 * 
	 * @return El puerto en el que escucha el servidor, o 0 en caso de error.
	 */
	protected int getServerPort() {
		int port = 0;
		/*
		 * Devolver el puerto de escucha de nuestro servidor de ficheros
		 */

		if (fileServer != null) {
	        return fileServer.getServerPort();
	    }
		return port;
	}

	/**
	 * Método para detener nuestro servidor de ficheros en segundo plano
	 * 
	 */
	protected void stopFileServer() {
		/*
		 * Enviar señal para detener nuestro servidor de ficheros en segundo plano
		 */
		if (fileServer != null) {
	        fileServer.stopServer();
	        fileServer = null; 
	        System.out.println("[stopFileServer] File server stopped.");
	    } else {
	        System.out.println("[stopFileServer] No server was running.");
	    }
	}

	protected boolean serving() {
		boolean result = false;
		// Si fileServer existe y está corriendo, devuelve true
	    if (fileServer != null && fileServer.isRunning()) {
	    	 result = true;
	    }
		return result;

	}

}
