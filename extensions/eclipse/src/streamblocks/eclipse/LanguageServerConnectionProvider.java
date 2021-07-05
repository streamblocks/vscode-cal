package streamblocks.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.services.LanguageServer;
import org.osgi.framework.Bundle;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.lsp4e.LanguageServerPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

@SuppressWarnings("restriction")
public class LanguageServerConnectionProvider implements StreamConnectionProvider {

	private static enum ConnectionType { CONNECTION_PROCESS_IO, CONNECTION_SOCKET };
	
	private static final ConnectionType connectionType = ConnectionType.CONNECTION_PROCESS_IO;
	
	private static final int CONNECTION_SOCKET_PORT = 5008;

	private StreamConnectionProvider provider;

	public LanguageServerConnectionProvider() {
		switch (connectionType) {
			case CONNECTION_PROCESS_IO:
				List<String> commands = new ArrayList<>();
				commands.add("java");
				//commands.add("-Xdebug");
				//commands.add("-Xrunjdwp:server=y,transport=dt_socket,address=4001,suspend=n,quiet=y");
				commands.add("-jar");
				Bundle bundle = Activator.getContext().getBundle();
				Path workingDir = Path.EMPTY;
				try {
					workingDir = new Path(FileLocator.toFileURL(FileLocator.find(bundle, new Path("lib"), null)).getPath());
					commands.add(workingDir.append("/xtext-language-server.jar").toOSString());
				} catch (IOException e) {
					LanguageServerPlugin.logError(e);
				}
				provider = new ProcessStreamConnectionProvider(commands, workingDir.toOSString()) {};
				break;
			case CONNECTION_SOCKET:
			default:
				provider = new SocketStreamConnectionProvider(CONNECTION_SOCKET_PORT);
				break;
		}
	}

	@Override
	public void start() throws IOException {
		provider.start();
	}
	
	@Override
	public InputStream getErrorStream() {
		return provider.getErrorStream();
	}

	@Override
	public InputStream getInputStream() {
		return provider.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return provider.getOutputStream();
	}
	
	@Override
	public Object getInitializationOptions(URI rootUri) {
		return provider.getInitializationOptions(rootUri);
	}
	
	@Override
	public void handleMessage(Message message, LanguageServer languageServer, URI rootURI) {
		provider.handleMessage(message, languageServer, rootURI);
	}

	@Override
	public void stop() {
		provider.stop();
	}

}