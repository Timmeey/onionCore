package de.timmeey.anoBitT.dht.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import anoBitT.DHTGetRequest;
import anoBitT.DHTPutRequest;
import anoBitT.DHTReply;

import com.google.inject.Inject;

import timmeeyLib.exceptions.unchecked.NotYetImplementedException;
import timmeeyLib.properties.PropertiesAccessor;
import de.timmeey.anoBitT.communication.HTTPRequestService;
import de.timmeey.anoBitT.config.GuiceAnnotations.DHTExternalPort;
import de.timmeey.anoBitT.config.GuiceAnnotations.DHTProperties;
import de.timmeey.anoBitT.dht.DHTService;
import de.timmeey.anoBitT.network.SocketFactory;

public class DHTServiceFakeImpl implements DHTService {

	private final SocketFactory socketFactory;
	private final PropertiesAccessor props;
	private final HTTPRequestService requestService;
	private final int dhtRequestServerPort;

	@Inject
	protected DHTServiceFakeImpl(SocketFactory socketFactory,
			@DHTProperties PropertiesAccessor props,
			HTTPRequestService requestService,
			@DHTExternalPort int dhtRequestServerPort) {
		this.props = props;
		this.socketFactory = socketFactory;
		this.requestService = requestService;
		this.dhtRequestServerPort = dhtRequestServerPort;

	}

	@Override
	public boolean put(String key, String value, boolean waitForConfirmation) {
		boolean result = !waitForConfirmation;
		DHTPutRequest putRequest = new DHTPutRequest(
				props.getProperty("DHTHostname"), key, value);

		// DHTPutRequest putRequest = new DHTPutRequest("localhost", key,
		// value);
		Future<DHTReply> putReply = requestService.send(putRequest,
				putRequest.getResponseType(), dhtRequestServerPort);
		System.out.println("done");
		if (waitForConfirmation) {
			DHTReply realReply;
			try {
				realReply = putReply.get(60, TimeUnit.SECONDS);
				result = realReply.getKey().equals(key)
						&& realReply.getValue().equals(value);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // catch (TimeoutException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
			catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return result;
	}

	public String get(String key) {
		String result = null;
		DHTGetRequest getRequest = new DHTGetRequest(
				props.getProperty("DHTHostname"), key);
		// DHTGetRequest getRequest = new DHTGetRequest("localhost", key);

		Future<DHTReply> getReply = requestService.send(getRequest,
				getRequest.getResponseType(), dhtRequestServerPort);
		try {
			result = getReply.get(60, TimeUnit.SECONDS).getValue();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	public List<String> getNodes() {
		throw new NotYetImplementedException(
				"In this implementation of DHTService not available");
	}
}