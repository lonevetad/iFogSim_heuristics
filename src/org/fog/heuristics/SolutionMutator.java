package org.fog.heuristics;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * 
 * @author marcoottina (marco.1995.ottina@gmail.com )
 *
 * @param <FragmentOfSolution>
 * @param <Sol>
 */
public interface SolutionMutator<FragmentOfSolution, Sol extends Supplier<List<FragmentOfSolution>>> {

	/**
	 * Mutates the part of a solution into some new value
	 * 
	 * @param originalFragment the part (of the solution) to be mutated into
	 *                         something else
	 * @param solutionContext  the solution from which the given part comes from.
	 *                         It's just a context, may be useful.
	 * @param heuristicContext the heuristic running this mutator. It's just a
	 *                         context, may be useful.
	 * @param r                an instance of {@link Random}
	 * @return
	 */
	public FragmentOfSolution mutateFragmentOfSolution(FragmentOfSolution originalFragment, Sol solutionContext,
			Heuristic<Sol> heuristicContext, Random r);
}
