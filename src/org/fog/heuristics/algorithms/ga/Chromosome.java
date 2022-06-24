package org.fog.heuristics.algorithms.ga;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * A Chromosome is a {@link List} of elements (see {@łink #getGenes()}) able to
 * be manipulated (see {@link #crossoverOnePoint(Chromosome, Random)} and
 * {@link #crossoverKPoint(Chromosome, int, Random)}).
 */
public abstract class Chromosome<T> implements Cloneable, Supplier<List<T>> {

	public Chromosome() {
		super();
	}

	public Chromosome(List<T> genes) {
		super();
		this.setGenes(genes);
	}

	public abstract List<T> getGenes();

	@Override
	public final List<T> get() {
		return this.getGenes();
	}

	public abstract void setGenes(List<T> genes);

	//

	public void swapGeneAt(Chromosome<T> c, int index) {
		T temp;
		List<T> genes, cGenes;
		genes = this.getGenes();
		cGenes = c.getGenes();
		if (c == null || this == c || cGenes.size() != genes.size()) {
			return;
		}
		temp = genes.get(index);
		genes.set(index, cGenes.get(index));
		cGenes.set(index, temp);
	}

	/**
	 * Performs the crossover between this Chromosome and another one. The crossover
	 * point is selected randomly (hence the second parameter) and it's limited to
	 * just a single crossover point. K-point crossover will be implemented in
	 * further development.<br>
	 * The section to swap (the one above or below the crossover point) is choosen
	 * randomly
	 */
	public void crossoverOnePoint(Chromosome<T> c, Random r) {
		int size, indexCrossover;
		List<T> genes, cGenes;
		genes = this.getGenes();
		cGenes = c.getGenes();
		size = genes.size();
		if (c == null || this == c || cGenes.size() != size) {
			return;
		}
		if (size <= 0) {
			throw new RuntimeException(
					"No genes to crossover in Genetic Algorithm's Chromosome; genes amount: " + size);
		}
		indexCrossover = r.nextInt(size);
		if (indexCrossover == 0 || indexCrossover == (size - 1)) {
			List<T> temp;
			temp = genes;
			genes = cGenes;
			cGenes = temp;
			return;
		}
		if (r.nextBoolean()) {
			// upper - indexCrossover index is excluded
			T temp;
			while (--indexCrossover >= 0) {
				temp = genes.get(indexCrossover);
				genes.set(indexCrossover, cGenes.get(indexCrossover));
				cGenes.set(indexCrossover, temp);
			}
		} else {
			// lower - indexCrossover index is included
			T temp;
			while (indexCrossover <= --size) {
				temp = genes.get(size);
				genes.set(size, cGenes.get(size));
				cGenes.set(size, temp);
			}
		}
	}

	/**
	 * Similar to {@link #crossoverOnePoint(Chromosome, Random)}, but swaps sections
	 * of genes in a alternate way: one section is swapped with the other
	 * chromosome, the next one is kept, the subsequent one is swapped, and so on.
	 * 
	 * @param c the {@link Chromosome} to perform the crossover with
	 * @param k amount of sections to crossover. If this number is non-positive,
	 *          then nothing happens. If it's {@code 1}, then
	 *          {@link #crossoverOnePoint(Chromosome, Random)} is called. If it's
	 *          greated than the amount of genes, then a simple random swapping is
	 *          performed. Otherwise, a proper crossover is performed.
	 * @param r just a {@link Random} instance, must be non null
	 */
	public void crossoverKPoint(Chromosome<T> c, int k, Random r) {
		int size, indexCrossover, indexes[];
		T temp;
		List<T> genes, cGenes;
		if (k <= 0) {
			return;
		}
		if (k == 1) {
			crossoverOnePoint(c, r);
			return;
		}

		genes = this.getGenes();
		cGenes = c.getGenes();
		size = genes.size();
		if (c == null || this == c || cGenes.size() != size) {
			return;
		}

		Objects.requireNonNull(r);

		if (k >= size) {
			// random flip
			while (--size >= 0) {
				if (r.nextBoolean()) {
					temp = genes.get(size);
					genes.set(size, cGenes.get(size));
					cGenes.set(size, temp);
				}
			}
			return;
		}

		indexes = new int[k];
		{
			SortedSet<Integer> setIndexes;
			setIndexes = new TreeSet<>();
			while (--k >= 0) {
				while (setIndexes.contains(indexCrossover = r.nextInt(size)))
					;
				setIndexes.add(indexCrossover);
			}
			for (Integer i : setIndexes) {
				indexes[k++] = i;
			}
			setIndexes = null;
		}

		/**
		 * By describing a k-point crossover with the metaphor of curling two strings in
		 * a progressive way,<br>
		 * then each resulting chromosome will have alternating sections of original and
		 * swapped genes. <br>
		 * The following test determines if the starting section (the last one for
		 * simplicity of the code <br>
		 * should be swapped or not, therefore defining the whole sequence. <br>
		 * The "kept" section are, in fact, skipped.
		 */
		if (r.nextBoolean()) {
			// if the last section should be the "original", then it has to be skipped
			k--;
		}
		do {
			indexCrossover = indexes[k];
			// recycle the "size" variable as a "start of the section"
			size = (k == 0) ? -1 : indexes[k - 1];
			while (indexCrossover > size) {
				temp = genes.get(indexCrossover);
				genes.set(indexCrossover, cGenes.get(indexCrossover));
				cGenes.set(indexCrossover, temp);
				indexCrossover--;
			}
		} while ((k -= 2) >= 0);
	}

	protected abstract Chromosome<T> newInstance(List<T> genes);

	@Override
	public Object clone() {
		List<T> genes;
		final List<T> clonedGenes;
		genes = this.getGenes();
		clonedGenes = new ArrayList<T>(genes.size());
		genes.forEach(clonedGenes::add);
		return this.newInstance(clonedGenes);
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = 1;
		result = prime * result + ((getGenes() == null) ? 0 : getGenes().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Chromosome<?>))
			return false;
		@SuppressWarnings("unchecked")
		Chromosome<T> other = (Chromosome<T>) obj;
		List<T> genes, othergenes;
		genes = this.getGenes();
		othergenes = other.getGenes();
		if (genes == null) {
			if (othergenes != null)
				return false;
		} else if (!genes.equals(othergenes))
			return false;
		return true;
	}

}
