package de.timmeey.anoBitT.peerGroup;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.timmeey.anoBitT.main;
import de.timmeey.anoBitT.peerGroup.Member.PeerGroupMember;
import de.timmeey.anoBitT.tor.KeyPair;
import de.timmeey.libTimmeey.networking.communicationClient.HTTPRequestService;

public class PeerGroup {
	private static final Logger logger = LoggerFactory
			.getLogger(PeerGroup.class);
	private String name;
	private final UUID groupId;
	private Set<PeerGroupMember> members;
	private final Object MEMBERS_SYNC = new Object();
	private final int MAXWAINTINGFACTOR = 15;
	private final KeyPair keyPair;
	private final HTTPRequestService requestService;

	public PeerGroup(String name, KeyPair keyPair,
			HTTPRequestService requestService) {

		this.groupId = UUID.randomUUID();
		members = Sets.newHashSet();
		this.keyPair = keyPair;
		this.requestService = requestService;
	}

	/**
	 * Get the unique name of the group
	 * 
	 * @return the unique group name
	 */
	public UUID getGroupId() {
		return this.groupId;
	}

	/**
	 * Gets the humanReadable Name of the group
	 * 
	 * @return the human readable name of the group
	 */
	public String getHumanReadableGroupName() {
		return this.name;
	}

	/**
	 * Returns all the current member
	 * 
	 * @return current member
	 */
	public Set<PeerGroupMember> getMembers() {
		return this.members;
	}

	// /**
	// * Returns the time of the last List update
	// *
	// * @return the update time
	// */
	// public long getLastUpdateTime() {
	// }

	/**
	 * Gets a specific group member by his onion address, or null if not found
	 * 
	 * @param onionAddress
	 *            the onion address of the wanted user
	 * @return the specified user or null
	 */
	public Optional<PeerGroupMember> getMember(String onionAddress) {
		return members.stream()
				.filter(m -> m.getOnionAddress().equals(onionAddress))
				.findFirst();
	}

	/**
	 * Adds a Member to a PeerGroup.
	 * 
	 * @param member
	 *            The new member
	 * @return the peerGroup with the added member
	 */
	public PeerGroup addMember(PeerGroupMember member) {
		synchronized (MEMBERS_SYNC) {
			this.members.add(member);
			return this;
		}

	}

	/**
	 * Updates the PeerGroup by querying other members for updated information
	 * Intended to be called while starting the application
	 * 
	 * @return A Future with the updated PeerGroup
	 */
	public PeerGroup updateGroupMembers() {
		synchronized (MEMBERS_SYNC) {

			List<Future<PeerGroupUpdateResponse>> memberFutureLists = members
					.stream().map(m -> getUpdatedMemberListFromMebmber(m))
					.collect(Collectors.toList());

			List<Set<PeerGroupMember>> memberLists = Lists.newArrayList();

			long waitUntil = System.currentTimeMillis() + MAXWAINTINGFACTOR
					* 1000;
			for (Future<PeerGroupUpdateResponse> members : memberFutureLists) {
				try {
					Set<PeerGroupMember> tmpSet = members
							.get(waitUntil - System.currentTimeMillis(),
									TimeUnit.MILLISECONDS).getPeerGroup()
							.getMembers();
					memberLists.add(tmpSet);
				} catch (InterruptedException | ExecutionException
						| TimeoutException e) {
					members.cancel(true);
					logger.info("Could not get response from Member for new PeerGroupMember list");
				}
			}
			this.members = resolvePossibleConflicts(memberLists);

			return this;
		}
	}

	private Future<PeerGroupUpdateResponse> getUpdatedMemberListFromMebmber(
			PeerGroupMember m) {
		PeerGroupUpdateRequest request = new PeerGroupUpdateRequest(m,
				this.keyPair);
		return this.requestService.send(request, request.getResponseType(),
				8888);
	}

	/**
	 * Updates the online status of all group Members bz asking them for their
	 * IP Intended to be called on startup or periodically
	 * 
	 * @return A future with the updated peerGroup
	 */
	public PeerGroup updateOnlineStats() {
		members.stream().parallel().forEach(PeerGroupMember::updateIpAddress);
		return this;
	}

	/**
	 * Due to the restriction that there can't be removals of members, allways
	 * the list with the most members is considered the most recent
	 * 
	 * @param memberLists
	 * @return the most recent member list
	 */

	private Set<PeerGroupMember> resolvePossibleConflicts(
			List<Set<PeerGroupMember>> memberLists) {
		// return
		// memberLists.stream().max(Comparator.comparing(List::size)).get();
		return memberLists.stream().flatMap(ml -> ml.stream())
				.collect(Collectors.toSet());
	}

}
