package com.bismuth.requestergolems;

/**
 * Persistent role state added to vanilla chest block entities.
 *
 * <p>The underlying block and inventory remain vanilla. Requester mode is
 * metadata layered on top, which lets the same Copper Chest later expose
 * requester controls without introducing a second chest block.</p>
 */
public interface RequesterChestAccess {
	boolean requestergolems$isRequester();

	void requestergolems$setRequester(boolean requester);
}
