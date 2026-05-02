package es.um.redes.nanoFiles.tcp.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import es.um.redes.nanoFiles.udp.message.DirMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class PeerMessage {
	private byte opcode;

	/*
	 * (Boletín MensajesBinarios) Añadir atributos u otros constructores
	 * específicos para crear mensajes con otros campos, según sea necesario
	 * 
	 */
	private FileInfo[] peerfiles;	// GUARDAR LISTA DE FICHEROS opcode 2
	
	// si se manda sin offset no caben mas de 65 bytes
	//private Long peerfileOffset;

	// NO ESTAMOS INCLUYENDO EL OFFSET, TAMAÑO Y NOMBRE PORQUE EN EL FORMATO NO LOS HEMOS PUESTO
    private String peerfileSubhash;     // Para guardar el hash solicitado (Opcode 3)
    private byte[] peerfileData;		// Para guardar el fichero descargado
    
    // PARA EL NICKNAME DE PEERDL Y PEERFILES
    private String nicknamep2p;

	public PeerMessage() {
		opcode = PeerMessageOps.OPCODE_INVALID_CODE;
	}

	public PeerMessage(byte op) {
		opcode = op;
	}
	
	public PeerMessage(byte op, FileInfo[] ficheros) {
		assert(op==PeerMessageOps.OPCODE_PEER_FILES_OK);
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
	 * (Boletín MensajesBinarios) Crear métodos getter y setter para obtener
	 * los valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".		SE HA COMPROBADO CON LA FUNCION checkValidOpcode, con los opcodes de PeerMessageOps
	 */
	
	public byte getOpcode() {
        return opcode;
    }

    public void setOpcode(byte opcode) {
        this.opcode = opcode;
    }

    public String getPeerfileSubhash() {
        checkValidOpcode("fileHash", PeerMessageOps.OPCODE_PEER_DL);
        return peerfileSubhash;
    }

    public void setPeerfileSubhash(String fileHash) {
        checkValidOpcode("fileHash", PeerMessageOps.OPCODE_PEER_DL);
        this.peerfileSubhash = fileHash;
    }

    public byte[] getPeerfileData() {
        checkValidOpcode("fileData", PeerMessageOps.OPCODE_PEER_DL_OK);
        return peerfileData;
    }

    public void setPeerfileData(byte[] fileData) {
        checkValidOpcode("fileData", PeerMessageOps.OPCODE_PEER_DL_OK);
        this.peerfileData = fileData;
    }
    
	
    public String getNicknamep2p() {
		return nicknamep2p;
	}

	public void setNicknamep2p(String nicknamep2p) {
		this.nicknamep2p = nicknamep2p;
	}

	/**
     * Método auxiliar para validar si el opcode actual permite el acceso al campo.
     * @param fieldName Nombre del atributo que se intenta acceder.
     * @param validOpcodes Lista de opcodes en los que este campo tiene sentido.
     */
    private void checkValidOpcode(String fieldName, byte... validOpcodes) {
        if (this.opcode == PeerMessageOps.OPCODE_INVALID_CODE) {
            throw new IllegalStateException("No se puede acceder a '" + fieldName + "' porque el opcode no ha sido inicializado (INVALID_CODE).");
        }
        
        for (byte valid : validOpcodes) {
            if (this.opcode == valid) {
                return;
            }
        }
        
        throw new IllegalStateException("El campo '" + fieldName + 
            "' no está definido para el tipo de mensaje actual (opcode: " + this.opcode + ")");
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
		 * (Boletín MensajesBinarios) En función del tipo de mensaje, leer del
		 * socket a través del "dis" el resto de campos para ir extrayendo con los
		 * valores y establecer los atributos del un objeto DirMessage que contendrá
		 * toda la información del mensaje, y que será devuelto como resultado. NOTA:
		 * Usar dis.readFully para leer un array de bytes, dis.readInt para leer un
		 * entero, etc.
		 */
		PeerMessage message = new PeerMessage();
		byte opcode = dis.readByte();
		message.setOpcode(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES:
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_OK:
			int tamanyo = dis.readInt();
			byte[] bDatos = new byte[tamanyo];
			dis.readFully(bDatos);
			message.setPeerfiles(FileInfo.deserializeList(bDatos));
			break;
		case PeerMessageOps.OPCODE_PEER_DL:
			int tamanyoHash = dis.readInt();   // 8 bytes
            byte[] bytesHash = new byte[tamanyoHash];
            dis.readFully(bytesHash);            // n bytes
            message.setPeerfileSubhash(new String(bytesHash));
			break;
		case PeerMessageOps.OPCODE_PEER_DL_OK:
			int longitudFichero = dis.readInt(); // 8 bytes
            byte[] datosFichero = new byte[longitudFichero];
            dis.readFully(datosFichero);           // n bytes
            message.setPeerfileData(datosFichero);
			break;
		case PeerMessageOps.OPCODE_PEER_DL_ERROR_CONCORDANCIA:
			break;	
		case PeerMessageOps.OPCODE_PEER_DL_ERROR_AMBIGUEDAD:
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
		 * (Boletín MensajesBinarios): Escribir los bytes en los que se codifica el
		 * mensaje en el socket a través del "dos", teniendo en cuenta opcode del
		 * mensaje del que se trata y los campos relevantes en cada caso. NOTA: Usar
		 * dos.write para leer un array de bytes, dos.writeInt para escribir un entero,
		 * etc.
		 */


		dos.writeByte(opcode);
		switch (opcode) {
		case PeerMessageOps.OPCODE_PEER_FILES:
			break;
		case PeerMessageOps.OPCODE_PEER_FILES_OK:
			// consigo array de bytes que lo representa
			byte[] bFiles = FileInfo.serializeList(getPeerfiles());
			// escribo campo longitud
			dos.writeInt(bFiles.length);
			dos.write(bFiles);
			
			break;
		case PeerMessageOps.OPCODE_PEER_DL:
			byte[] bHash = getPeerfileSubhash().getBytes();
			dos.writeInt(bHash.length);
			dos.write(bHash);
			break;
		case PeerMessageOps.OPCODE_PEER_DL_OK:
			byte[] bData = getPeerfileData();
			dos.writeInt(bData.length);
			dos.write(bData);
			break;
		case PeerMessageOps.OPCODE_PEER_DL_ERROR_CONCORDANCIA:
			break;	
		case PeerMessageOps.OPCODE_PEER_DL_ERROR_AMBIGUEDAD:
			break;
		default:
			System.err.println("PeerMessage.writeMessageToOutputStream found unexpected message opcode " + opcode + "("
					+ PeerMessageOps.opcodeToOperation(opcode) + ")");
		}
	}
	
	public String toDebugString() {
		StringBuffer sb = new StringBuffer();
	  //  sb.append("[PeerMessage.toDebugString()] DEBUG:\n"); 
	    
	    // Obtenemos el nombre de la operación usando tu clase PeerMessageOps
	    String opName = PeerMessageOps.opcodeToOperation(opcode);
	    sb.append("operation:").append(opName != null ? opName.toLowerCase() : "unknown").append("\n");
	    
	    switch (opcode) {
	    	case PeerMessageOps.OPCODE_PEER_FILES:{
	    		if (nicknamep2p != null) {
	    	        sb.append("peerfilenickname:").append(nicknamep2p).append("\n");
	    	    }
	    	}
	        case PeerMessageOps.OPCODE_PEER_FILES_OK:
	            if (peerfiles != null) {
		        	sb.append("peerfilenickname:").append(nicknamep2p).append("\n");
	                for (FileInfo f : peerfiles) {
	                    sb.append("peerfilename:" + f.fileName + "\n");
	                    sb.append("peerfilesubhash:" + f.fileHash + "\n");
	                    sb.append("peerfilesize:"+ f.fileSize +"\n");
	                }
	            }
	            break;
	        case PeerMessageOps.OPCODE_PEER_DL:
	        	if (nicknamep2p != null) {
	                sb.append("peerdlnickname:").append(nicknamep2p).append("\n");
	            }
	        	sb.append("peerdlhashsubstring:" + peerfileSubhash + "\n");
	            break;
	        case PeerMessageOps.OPCODE_PEER_DL_OK:
	            break;
	        case PeerMessageOps.OPCODE_PEER_DL_ERROR_CONCORDANCIA:
	            break;
	        case PeerMessageOps.OPCODE_PEER_DL_ERROR_AMBIGUEDAD:
	            break;
	    }
	    return sb.toString();
	}

}
