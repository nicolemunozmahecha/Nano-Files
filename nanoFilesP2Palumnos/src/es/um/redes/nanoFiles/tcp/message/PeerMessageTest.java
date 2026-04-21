package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessageTest {

	public static void main(String[] args) throws IOException {
		String nombreArchivo = "peermsg.bin";
		DataOutputStream fos = new DataOutputStream(new FileOutputStream(nombreArchivo));

		/*
		 * TProbar a crear diferentes tipos de mensajes (con los opcodes válidos
		 * definidos en PeerMessageOps), estableciendo los atributos adecuados a cada
		 * tipo de mensaje. Luego, escribir el mensaje a un fichero con
		 * writeMessageToOutputStream para comprobar que readMessageFromInputStream
		 * construye un mensaje idéntico al original.
		 */
		/*
		PeerMessage msgOut = new PeerMessage();
		msgOut.writeMessageToOutputStream(fos);*/

		DataInputStream fis = new DataInputStream(new FileInputStream(nombreArchivo));
		//PeerMessage msgIn = PeerMessage.readMessageFromInputStream((DataInputStream) fis);
		/*
		 *  Comprobar que coinciden los valores de los atributos relevantes al tipo
		 * de mensaje en ambos mensajes (msgOut y msgIn), empezando por el opcode.
		 */
		/*
		if (msgOut.getOpcode() != msgIn.getOpcode()) {
			System.err.println("Opcode does not match!");
		}*/
		// PARA PRUEBA METER PDF RANDOM EN DIR-SHARED
		FileInfo[] directoryFiles;
		String directoryFilesPath = "dir-shared";
		File dir = new File(directoryFilesPath);
		if(!dir.exists()) {
			dir.mkdirs();
		}
		directoryFiles = FileInfo.loadFilesFromFolder(directoryFilesPath);
		System.out.println("*Directoy loaded "+directoryFiles.length + " files from " + directoryFilesPath);
		
		// simulamos envio
		PeerMessage peerListReq = new PeerMessage(PeerMessageOps.OPCODE_PEER_FILES, directoryFiles);
		peerListReq.writeMessageToOutputStream(fos);
		
		PeerMessage peerListRep = PeerMessage.readMessageFromInputStream(fis);
		
		System.out.println("Codigo op enviado: " + peerListReq.getOpcode());
		System.out.println("Codigo op recibido: " + peerListRep.getOpcode());
		
		System.out.println("Lista ficheros enviados: " + peerListReq.getPeerfiles().toString());
		for(FileInfo f: peerListReq.getPeerfiles()) {
			System.out.println("Fichero:" + f.toString());
		}
		System.out.println("Lista ficheros recibidos: " + peerListRep.getPeerfiles().toString());
		for(FileInfo f: peerListRep.getPeerfiles()) {
			System.out.println("Fichero:" + f.toString());
		}
	}

}