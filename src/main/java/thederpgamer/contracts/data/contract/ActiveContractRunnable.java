package thederpgamer.contracts.data.contract;

import org.schema.game.common.data.player.PlayerState;

import java.util.List;

/**
 * Interface for contracts that have an active runnable to check for events.
 * <br/>Used for "railroaded" contracts that have specific event sequences that must be followed.
 *
 * @author Garret Reichenbach
 */
public interface ActiveContractRunnable {

	boolean canStartRunner(PlayerState player);

	List<?> startRunner(PlayerState player);

	boolean updateRunner(PlayerState player, List<?> data);
}
