package com.bismuth.requestergolems;

/**
 * Server-side state contract added to vanilla Copper Golems.
 *
 * <p>The interface is intentionally small: roles and upgrades will build on
 * this foundation without requiring a separate custom golem entity.</p>
 */
public interface RequesterGolemAccess {
	boolean requestergolems$isRequester();

	void requestergolems$setRequester(boolean requester);
}
