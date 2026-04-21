package es.um.redes.nanoFiles.tcp.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import es.um.redes.nanoFiles.tcp.message.PeerMessage;
import es.um.redes.nanoFiles.tcp.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor
public class NFConnector {
	private Socket socket;
	private InetSocketAddress serverAddr;
	private DataInputStream dis;
	private DataOutputStream dos;



	public NFConnector(InetSocketAddress fserverAddr) throws UnknownHostException, IOException {
		serverAddr = fserverAddr;
		/*
		 * (Boletín SocketsTCP) Se crea el socket a partir de la dirección del
		 * servidor (IP, puerto). La creación exitosa del socket significa que la
		 * conexión TCP ha sido establecida.
		 */
		socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
		/*
		 * (Boletín SocketsTCP) Se crean los DataInputStream/DataOutputStream a
		 * partir de los streams de entrada/salida del socket creado. Se usarán para
		 * enviar (dos) y recibir (dis) datos del servidor.
		 */
		dis = new DataInputStream(socket.getInputStream());
		dos = new DataOutputStream(socket.getOutputStream());
	}

	public void test() {
		/*
		 * (Boletín SocketsTCP) Enviar entero cualquiera a través del socket y
		 * después recibir otro entero, comprobando que se trata del mismo valor.
		 */
		// mandar con dis escribir con dos mandar con dis
		System.out.println("[DEBUG] Entra en NFConnector ");
		try {
			dos.writeInt(55);
			int numero = dis.readInt();
			if(numero == 55) {
				System.out.println("Número correcto!");
			}else {
				System.out.println("Número incorrecto!");
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}


	public InetSocketAddress getServerAddr() {
		return serverAddr;
	}

	public FileInfo[] getFileList() throws IOException {
		try {
	        PeerMessage peticion = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILES);
	        peticion.writeMessageToOutputStream(dos);
	        
	        PeerMessage respuesta = PeerMessage.readMessageFromInputStream(dis);
	        System.out.println(respuesta.toDebugString());
	        
	        if (respuesta.getOpcode() == PeerMessageOps.OPCODE_PEER_FILES_OK) {
	            return respuesta.getPeerfiles();
	        }
	    } catch (IOException e) {
	        System.err.println("[NFConnector] Error getting file list: " + e.getMessage());
	    }
	    return null;
	}
	
	public void close() throws IOException {
        if (socket != null) socket.close();
    }
	
	// siguiendo cuerpo de getFileList
	public byte[] downloadFile(String targetHashSubstring) {
	    byte[] fileData = null;
	    try {
	        PeerMessage request = new PeerMessage(PeerMessageOps.OPCODE_PEER_DL);
	        request.setPeerfileSubhash(targetHashSubstring);
	        request.writeMessageToOutputStream(this.dos);

	        PeerMessage response = PeerMessage.readMessageFromInputStream(this.dis);
	        System.out.println(response.toDebugString());

	        if (response.getOpcode() == PeerMessageOps.OPCODE_PEER_DL_OK) {
	            fileData = response.getPeerfileData();
	            System.out.println("[downloadFile] Download successful, received " + fileData.length + " bytes.");
	            
	        } else if (response.getOpcode() == PeerMessageOps.OPCODE_PEER_DL_ERROR_CONCORDANCIA) {
	            System.err.println("[downloadFile] Server error: File not found (no concordance).");
	            		
	        } else if (response.getOpcode() == PeerMessageOps.OPCODE_PEER_DL_ERROR_AMBIGUEDAD) {
	            System.err.println("[downloadFile] Server error: Multiple files match that hash substring.");
	            
	        } else {
	            System.err.println("[downloadFile] Server returned unexpected opcode: " + response.getOpcode());
	        }

	    } catch (IOException e) {
	        System.err.println("[downloadFile] Error during file download: " + e.getMessage());
	    }
	    
	    return fileData;
	}
}
