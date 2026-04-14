package es.um.redes.nanoFiles.udp.message;

public class DirMessageOps {

	/*
	 * TODO: (Boletín MensajesASCII) Añadir aquí todas las constantes que definen
	 * los diferentes tipos de mensajes del protocolo de comunicación con el
	 * directorio (valores posibles del campo "operation").
	 */
	
	// PING
	public static final String OPERATION_INVALID = "invalid_operation";
	public static final String OPERATION_PING = "ping";
	
	// TODO: definir las operaciones del protocolo de directorio
	public static final String OPERATION_PING_OK = "ping_ok";
	public static final String OPERATION_PING_ERROR = "ping_error";
	
	// DIRFILES
	public static final String OPERATION_DIRFILES = "dirfiles";
	public static final String OPERATION_DIRFILES_OK = "dirfiles_ok";

	// PEERS
	public static final String OPERATION_PEERS = "peers";
	public static final String OPERATION_PEERS_OK = "peers_ok";
	
	// SERVE
	public static final String OPERATION_SERVE = "serve";
	public static final String OPERATION_SERVE_OK = "serve_ok";
	
	// PEERFILES
	public static final String OPERATION_PEERFILES = "peerfiles";
	public static final String OPERATION_PEERFILES_OK = "peerfiles_ok";

	// PEERDL
	public static final String OPERATION_PEERDL = "peerdl";
	public static final String OPERATION_PEERDL_OK = "peerdl_ok";
	public static final String OPERATION_PEERDL_ERROR_CONC = "peerdl_error_concordancia";
	public static final String OPERATION_PEERDL_ERROR_AMB= "peerdl_error_ambiguedad";
	
	// AMPLIACIÓN DIRDL
	public static final String OPERATION_DIRDL = "dirdl";
	public static final String OPERATION_DIRDL_OK = "dirdl_ok";
	public static final String OPERATION_DIRDL_ERROR = "dirdl_error";

}
 