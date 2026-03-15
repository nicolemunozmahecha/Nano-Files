package es.um.redes.nanoFiles.udp.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import es.um.redes.nanoFiles.util.FileInfo;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases DirectoryServer y
 * DirectoryConnector, y se codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class DirMessage {
	public static final int PACKET_MAX_SIZE = 65507; // 65535 - 8 (UDP header) - 20 (IP header)

	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea
	//private static final char SEPARADOR = ','; // Para datos compuestos
	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	/*
	 * TODO: (Boletín MensajesASCII) Definir de manera simbólica los nombres de
	 * todos los campos que pueden aparecer en los mensajes de este protocolo
	 * (formato campo:valor) --> protocolo: ping, dirId: hash
	 */
	 
	private static final String FIELDNAME_PROTOCOLID = "protocolid";
	
	private static final String FIELDNAME_FILENAME = "filename";
	private static final String FIELDNAME_FILESIZE = "filesize";
	private static final String FIELDNAME_FILEHASH = "filehash";
	
	private static final String FIELDNAME_PEERNICKNAME = "peernickname";
	private static final String FIELDNAME_PEERIP = "peerip";
	private static final String FIELDNAME_PEERPORT = "peerport";
	
	private static final String FIELDNAME_SERVEPORT = "serveport";
	private static final String FIELDNAME_SERVEPEER = "servepeer";
	private static final String FIELDNAME_SERVEIP = "serveip";



	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation = DirMessageOps.OPERATION_INVALID;
	/**
	 * Identificador de protocolo usado, para comprobar compatibilidad del directorio.
	 */
	private String protocolId;
	/*
	 * TODO: (Boletín MensajesASCII) Crear un atributo correspondiente a cada uno de
	 * los campos de los diferentes mensajes de este protocolo.
	 */

	private FileInfo[] filelist;
	
	private Map<String, InetSocketAddress> peers;
	
	int servePort;
	String serveNombrePeer;
	String serveIp;
	
	public DirMessage(String op) {
		operation = op;
	}

	/*
	 * TODO: (Boletín MensajesASCII) Crear diferentes constructores adecuados para
	 * construir mensajes de diferentes tipos con sus correspondientes argumentos
	 * (campos del mensaje)
	 */
	public DirMessage(String op, FileInfo[] lista) {
		this(op);
		filelist = lista;
	}
	
	public DirMessage(String op, Map<String, InetSocketAddress> lista) {
		this(op);
		peers = lista;
	}
	
	public DirMessage(String op, String nombre, String ip, int puerto) {
		this(op);
		serveNombrePeer = nombre;
		serveIp = ip;
		servePort = puerto;
	}
	/*
	 * TODO: (Boletín MensajesASCII) Crear métodos getter y setter para obtener los
	 * valores de los atributos de un mensaje. Se aconseja incluir código que
	 * compruebe que no se modifica/obtiene el valor de un campo (atributo) que no
	 * esté definido para el tipo de mensaje dado por "operation".
	 */
	
	public String getOperation() {
		return operation;
	}
	
	public void setProtocolID(String protocolIdent) {
		if (!operation.equals(DirMessageOps.OPERATION_PING)) {
			throw new RuntimeException("DirMessage: setProtocolId called for message of unexpected type (" + operation + ")");
		}
		protocolId = protocolIdent;
	}

	public String getProtocolId() {
		return protocolId;
	}


	public FileInfo[] getFilelist() {
		return filelist;
	}
	

	public void setFilelist(FileInfo[] filelist) {
		this.filelist = filelist;
	}
	

	public Map<String, InetSocketAddress> getPeers() {
		return peers;
	}

	public void setPeers(Map<String, InetSocketAddress> peers) {
		this.peers = peers;
	}
	
	public int getServePort() {
		return servePort;
	}

	public void setServePort(int servePort) {
		this.servePort = servePort;
	}

	public String getServeNombrePeer() {
		return serveNombrePeer;
	}

	public void setServeNombrePeer(String serveNombrePeer) {
		this.serveNombrePeer = serveNombrePeer;
	}

	public String getServeIp() {
		return serveIp;
	}

	public void setServeIp(String serveIp) {
		this.serveIp = serveIp;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 */
	public static DirMessage fromString(String message) {
		/*
		 * TODO: (Boletín MensajesASCII) Usar un bucle para parsear el mensaje línea a
		 * línea, extrayendo para cada línea el nombre del campo y el valor, usando el
		 * delimitador DELIMITER, y guardarlo en variables locales.
		 */

		String[] lines = message.split(END_LINE + "");
		// Local variables to save data during parsing
		DirMessage m = null;
		ArrayList<FileInfo> temporal = new ArrayList<>();
		FileInfo aux = new FileInfo();
		Map<String, InetSocketAddress> mapaTemp = new LinkedHashMap<String, InetSocketAddress>();
		String cadAux = null;
		String ipAux = null;
		
		System.out.println("[DirMessage.fromString()] DEBUG:");
		for (String line : lines) {
			System.out.println("[DirMessage] DEBUG: Linea: " + line);
			
			int idx = line.indexOf(DELIMITER); // Posición del delimitador
			String fieldName = line.substring(0, idx).toLowerCase(); // minúsculas
			String value = line.substring(idx + 1).trim();			// valor que hay después de los 2 puntos.
			System.out.println("[fromString] DEBUG: fieldname: " + fieldName + "\n[fromString] DEBUG: value: " + value);
			switch (fieldName) {
			case FIELDNAME_OPERATION: {
				assert (m == null);
				m = new DirMessage(value);
				break;
			}
			case FIELDNAME_PROTOCOLID:{
				m.setProtocolID(value);
				break;
			}
			case FIELDNAME_FILENAME:{
				aux = new FileInfo();
				aux.fileName = value;
				break;
			}
			case FIELDNAME_FILEHASH:{
				aux.fileHash = value;
				break;
			}
			case FIELDNAME_FILESIZE:{
				aux.fileSize = Long.valueOf(value);
				temporal.add(aux);
				break;
			}
			case FIELDNAME_PEERNICKNAME:{
				cadAux = value;
				break;
			}
			case FIELDNAME_PEERIP:{
				ipAux = value;
				break;
			}
			case FIELDNAME_PEERPORT:{
				int puerto = Integer.parseInt(value);
				InetSocketAddress pareja;
				try {
					pareja = new InetSocketAddress(InetAddress.getByName(ipAux), puerto);
					mapaTemp.put(cadAux, pareja);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				break;
			}case FIELDNAME_SERVEPEER :{
				m.serveNombrePeer = value;
				break;
			}case FIELDNAME_SERVEIP:{
				m.serveIp = value;
				break;
			}case FIELDNAME_SERVEPORT:{
				m.servePort = Integer.valueOf(value);
				break;
			}
			default:
				System.err.println("PANIC: DirMessage.fromString - message with unknown field name " + fieldName);
				System.err.println("Message was:\n" + message);
				System.exit(-1);
			}
		}
		
		if (m != null && m.getOperation().equals(DirMessageOps.OPERATION_DIRFILES_OK)) {
	        m.filelist = temporal.toArray(new FileInfo[0]);
	    }
		
		if (m != null && m.getOperation().equals(DirMessageOps.OPERATION_PEERS_OK)) {
	        m.peers = mapaTemp;
	    }

		return m;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION + DELIMITER + operation + END_LINE); // Construimos el campo
		/*
		 * TODO: (Boletín MensajesASCII) En función de la operación del mensaje, crear
		 * una cadena la operación y concatenar el resto de campos necesarios usando los
		 * valores de los atributos del objeto.
		 */
		String operacion = getOperation();
		switch(operacion) {
		case DirMessageOps.OPERATION_PING: {
			sb.append(FIELDNAME_PROTOCOLID + DELIMITER + protocolId + END_LINE); // Construimos el campo
			break;
		}
		case DirMessageOps.OPERATION_PING_OK: {
			break;
		}
		case DirMessageOps.OPERATION_PING_ERROR: {
			break;
		}
		case DirMessageOps.OPERATION_DIRFILES_OK: {
			for(FileInfo f : filelist) {
				sb.append(FIELDNAME_FILENAME + DELIMITER + f.fileName + END_LINE); 
				sb.append(FIELDNAME_FILEHASH + DELIMITER + f.fileHash + END_LINE); 
				sb.append(FIELDNAME_FILESIZE + DELIMITER + f.fileSize + END_LINE); 
			}

			break;
		}
		case DirMessageOps.OPERATION_DIRFILES: {
			break;
		}
		case DirMessageOps.OPERATION_PEERS: {
			break;
		}
		case DirMessageOps.OPERATION_PEERS_OK: {
			for(Map.Entry<String, InetSocketAddress> i : peers.entrySet()) {
				sb.append(FIELDNAME_PEERNICKNAME + DELIMITER + i.getKey() + END_LINE); 
				sb.append(FIELDNAME_PEERIP+ DELIMITER + i.getValue().getHostName() + END_LINE); 
				sb.append(FIELDNAME_PEERPORT + DELIMITER + i.getValue().getPort() + END_LINE); 
			}
			break;
		}case DirMessageOps.OPERATION_SERVE: {
			sb.append(FIELDNAME_SERVEPORT + DELIMITER + servePort + END_LINE);
			break;
		}case DirMessageOps.OPERATION_SERVE_OK: {
			sb.append(FIELDNAME_SERVEPEER + DELIMITER + serveNombrePeer + END_LINE);
			sb.append(FIELDNAME_SERVEIP + DELIMITER + serveIp + END_LINE);
			sb.append(FIELDNAME_SERVEPORT + DELIMITER + servePort + END_LINE);

			break;
		}
		default:
			System.err.println("PANIC: DirMessage.toString - message with unknown operation name " + operacion);
			System.exit(-1);
		
		}
		sb.append(END_LINE); // Marcamos el final del mensaje
		System.out.println("[toString] DEBUG:\nCampos unidos:\n" + sb.toString());
		return sb.toString();
	}

}
