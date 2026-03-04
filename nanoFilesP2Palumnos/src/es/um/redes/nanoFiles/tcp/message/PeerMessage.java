package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {
	private byte opcode;

	/*
	 * TODO: (Boletín MensajesBinarios) Añadir atributos u otros constructores
	 * específicos para crear mensajes con otros campos, según sea necesario
	 * 
	 */
	private FileInfo[] peerfiles;
	
	private String subhash;
	// si se manda sin offset no caben mas de 65 bytes
	private Long offset;
	private int length;
	private byte[] data;
	


	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}
	
	public PeerMessage(byte op, FileInfo[] ficheros) {
		assert(op==PeerMessageOps.OPCODE_PEER_FILES_REPLY);
		this.opcode = op;
		this.setPeerfiles(ficheros);
	}

	public FileInfo[] getPeerfiles() {
		return peerfiles;
	}

	public void setPeerfiles(FileInfo[] peerfiles) {
		this.peerfiles = peerfiles;
	}


	/*
	 * TODO: (Boletín MensajesBinarios) Crear métodos getter y setter para obtener
	 * los valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	public byte getOpcode() {
		return opcode;
	}
	
	public String getSubhash() {
		return subhash;
	}

	public void setSubhash(String subhash) {
		this.subhash = subhash;
	}

	public Long getOffset() {
		return offset;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El array de bytes recibido
	 * @return Un objeto de esta clase cuyos atributos contienen los datos del
	 *         mensaje recibido.
	 * @throws IOException
	 */
	public static PeerMessage readMessageFromInputStream(DataInputStream dis) throws IOException {
		/*
		 * TODO: (Boletín MensajesBinarios) En función del tipo de mensaje, leer del
		 * socket a través del "dis" el resto de campos para ir extrayendo con los
		 * valores y establecer los atributos del un objeto DirMessage que contendrá
		 * toda la información del mensaje, y que será devuelto como resultado. NOTA:
		 * Usar dis.readFully para leer un array de bytes, dis.readInt para leer un
		 * entero, etc.
		 */
		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES_REQ:
			;
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_REPLY:
			int tamanyo = dis.readInt();
			byte[] bDatos = new byte[tamanyo];
			dis.readFully(bDatos);
			message.setPeerfiles(FileInfo.deserializeList(bDatos));
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL:
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL_DATA:
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL_ERROR:
			break;		
		default:
			System.err.println("PeerMessage.readMessageFromInputStream doesn't know how to parse this message opcode: "
					+ PeerMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return message;
	}

	public void writeMessageToOutputStream(DataOutputStream dos) throws IOException {
		/*
		 * TODO (Boletín MensajesBinarios): Escribir los bytes en los que se codifica el
		 * mensaje en el socket a través del "dos", teniendo en cuenta opcode del
		 * mensaje del que se trata y los campos relevantes en cada caso. NOTA: Usar
		 * dos.write para leer un array de bytes, dos.writeInt para escribir un entero,
		 * etc.
		 */

		dos.writeByte(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES_REQ:
			;
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_REPLY:
			// consigo array de bytes que lo representa
			byte[] bFiles = FileInfo.serializeList(getPeerfiles());
			// escribo campo longitud
			dos.writeInt(bFiles.length);
			dos.write(bFiles);
			
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL:
			byte[] bHash = getSubhash().getBytes();
			dos.writeLong(offset);
			dos.writeInt(length);
			dos.writeInt(bHash.length);
			dos.write(bHash);
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL_DATA:
			byte[] bData = getData();
			dos.writeInt(bData.length);
			dos.write(bData);
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_DL_ERROR:
			break;
		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}

}
