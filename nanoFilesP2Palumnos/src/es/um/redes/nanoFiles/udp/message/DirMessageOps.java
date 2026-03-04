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

	// PEERFILES
	public static final String OPERATION_PEERFILES = "peerfiles";
	public static final String OPERATION_PEERFILES_OK = "peerfiles_ok";


}
 