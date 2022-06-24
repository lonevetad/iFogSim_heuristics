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
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 */
public class PieceOfSolution implements Cloneable {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((device == null) ? 0 : device.hashCode());
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PieceOfSolution))
			return false;
		PieceOfSolution other = (PieceOfSolution) obj;
		if (device == null) {
			if (other.device != null)
				return false;
		} else if (!device.equals(other.device))
			return false;
		if (module == null) {
			if (other.module != null)
				return false;
		} else if (!module.equals(other.module))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PieceOfSolution [module= \"" + module.getName() + "\" -> device= \"" + device.getName() + "\"]";
	}

	@Override
	public Object clone() {
		return new PieceOfSolution(module, device);
	}
}