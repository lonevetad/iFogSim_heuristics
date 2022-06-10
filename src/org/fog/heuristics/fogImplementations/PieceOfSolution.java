package org.fog.heuristics.fogImplementations;

import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.heuristics.Heuristic;

/**
 * Describes an atom of a solution, the final "decision" of a decision process
 * (a {@link Heuristic}.<br>
 * It's just a mapping between a {@code AppModule} and the {@code FogDevice}
 * that will run it.
 * 
 * @author marcoottina
 *
 */
public class PieceOfSolution {
	public PieceOfSolution(AppModule module, FogDevice device) {
		super();
		this.module = module;
		this.device = device;
	}

	protected AppModule module;
	protected FogDevice device;

	//

	public AppModule getModule() {
		return module;
	}

	public FogDevice getDevice() {
		return device;
	}

	//

	public void setModule(AppModule module) {
		this.module = module;
	}

	public void setDevice(FogDevice device) {
		this.device = device;
	}
}