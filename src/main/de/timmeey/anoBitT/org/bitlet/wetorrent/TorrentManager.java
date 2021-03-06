/*
 *              bitlet - Simple bittorrent library
 *  Copyright (C) 2008 Alessandro Bahgat Shehata, Daniele Castagna
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package de.timmeey.anoBitT.org.bitlet.wetorrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import de.timmeey.anoBitT.dht.DHTService;
import de.timmeey.anoBitT.org.bitlet.wetorrent.disk.PlainFileSystemTorrentDisk;
import de.timmeey.anoBitT.org.bitlet.wetorrent.disk.TorrentDisk;
import de.timmeey.anoBitT.org.bitlet.wetorrent.peer.IncomingPeerListener;
import de.timmeey.anoBitT.peerGroup.PeerGroupManager;
import de.timmeey.anoBitT.tor.KeyPair;
import de.timmeey.libTimmeey.networking.NetSerializer;
import de.timmeey.libTimmeey.networking.SocketFactory;

public class TorrentManager {

	private final int port;

	private DHTService dht;
	private NetSerializer gson;
	private PeerGroupManager peerGroupManager;
	private KeyPair keyPair;
	private long lastCall = 0l;
	private long lastByteCount = 0l;

	private final Tracker tracker;

	private SocketFactory torSocketFactory;

	private SocketFactory plainSocketFactory;
	private final IncomingPeerListener peerListener;
	boolean stopped = false;

	public TorrentManager(int port, DHTService dht, NetSerializer gson,
			PeerGroupManager peerGroupManager, KeyPair keyPair,
			SocketFactory torSocketFactory, SocketFactory plainSocketFactory)
			throws IOException, InterruptedException {

		this.port = checkNotNull(port);
		this.dht = checkNotNull(dht);
		this.gson = checkNotNull(gson);
		this.peerGroupManager = checkNotNull(peerGroupManager);
		this.keyPair = checkNotNull(keyPair);
		this.plainSocketFactory = checkNotNull(plainSocketFactory);
		this.torSocketFactory = checkNotNull(torSocketFactory);

		this.tracker = new Tracker(dht, gson, peerGroupManager, keyPair);
		peerListener = new IncomingPeerListener(torSocketFactory,
				plainSocketFactory, peerGroupManager, port);
		peerListener.start();

	}

	public void startTorrent(String filename) throws Exception {
		// read torrent filename from command line arg

		// Parse the metafile
		Metafile metafile = new Metafile(new BufferedInputStream(
				new FileInputStream(filename)));

		// Create the torrent disk, this is the destination where the torrent
		// file/s will be saved
		File folder = new File(System.getProperty("user.home") + "/torrent/");
		folder.mkdirs();

		TorrentDisk tdisk = new PlainFileSystemTorrentDisk(metafile, folder);
		tdisk.init();
		tdisk.resume();

		Torrent torrent = new Torrent(metafile, tdisk, peerListener, tracker,
				torSocketFactory, plainSocketFactory);
		if (torrent.startDownload()) {
			long startedFromBytes = torrent.getTorrentDisk().getCompleted();
			boolean alreadyStarted = false;
			long startTime = 0l;
			boolean alreadyStopped = false;
			while (!stopped) {

				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					break;
				}

				torrent.tick();
				if (!alreadyStarted
						&& startedFromBytes != torrent.getTorrentDisk()
								.getCompleted()) {
					startTime = System.currentTimeMillis();
					alreadyStarted = true;
				}
				if (!alreadyStopped && torrent.isCompleted()) {
					long stopTime = System.currentTimeMillis();
					alreadyStopped = true;
					long totalDuration = stopTime - startTime;
					long totalBytes = torrent.getTorrentDisk().getCompleted()
							- startedFromBytes;
					System.out.println(String.format(
							"Total time: %s, total bytes: %s, average B/s: %s",
							totalDuration, totalBytes, totalBytes
									/ (totalDuration / 1000)));

				}
				System.out.printf("Got %s peers, completed %d bytes\n", torrent
						.getPeersManager().getActivePeersNumber(), torrent
						.getTorrentDisk().getCompleted());
				if (!torrent.isCompleted()) {
					calculateBytePerSecond(torrent.getTorrentDisk()
							.getCompleted());
				}
			}

			torrent.interrupt();
			peerListener.interrupt();
		} else {
			System.out.println("Failed to start torrent");
			torrent.interrupt();
			peerListener.interrupt();
		}
	}

	public void calculateBytePerSecond(long completedBytes) {
		long transferredBytes = completedBytes - lastByteCount;
		long now = System.currentTimeMillis();
		long bytesPerSecond = transferredBytes / ((now - lastCall) / 1000);
		lastByteCount = completedBytes;
		lastCall = now;
		System.out.println(bytesPerSecond + "B/s");
		System.out.println(bytesPerSecond / 1024 + "kB/s");
		System.out.println((bytesPerSecond / 1024 / 1024 + "MB/s"));

	}

	public void stop() {
		System.out.println("Stopping torrents");
		this.stopped = true;

	}

}
