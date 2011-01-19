package hu.pagavcs.bl;

import javax.net.ssl.TrustManager;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;

public class ShortTimeoutAuthenticationManager implements ISVNAuthenticationManager {

	private final ISVNAuthenticationManager delegate;
	private final int                       readTimeout;
	private final int                       connectionTimeout;

	/**
	 * @param delegate
	 * @param readTimeout
	 *            in milliseconds
	 * @param connectionTimeout
	 *            in milliseconds
	 */
	public ShortTimeoutAuthenticationManager(ISVNAuthenticationManager delegate, int readTimeout, int connectionTimeout) {
		this.delegate = delegate;
		this.readTimeout = readTimeout;
		this.connectionTimeout = connectionTimeout;
	}

	public int getReadTimeout(SVNRepository repository) {
		String protocol = repository.getLocation().getProtocol();
		if ("http".equals(protocol) || "https".equals(protocol)) {
			return readTimeout;
		}
		return 0;
	}

	public int getConnectTimeout(SVNRepository repository) {
		return connectionTimeout;
	}

	public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication)
	        throws SVNException {
		delegate.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
	}

	public void acknowledgeTrustManager(TrustManager manager) {
		delegate.acknowledgeTrustManager(manager);

	}

	public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
		return delegate.getFirstAuthentication(kind, realm, url);
	}

	public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
		return delegate.getNextAuthentication(kind, realm, url);
	}

	public ISVNProxyManager getProxyManager(SVNURL url) throws SVNException {
		return delegate.getProxyManager(url);
	}

	public TrustManager getTrustManager(SVNURL url) throws SVNException {
		return delegate.getTrustManager(url);
	}

	public boolean isAuthenticationForced() {
		return delegate.isAuthenticationForced();
	}

	public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
		delegate.setAuthenticationProvider(provider);
	}
}
