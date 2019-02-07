/**
 * 
 */
package ddl.reorder.mysql.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author vadherak
 *
 */
@Data
public class Table implements Comparable<Table> {

	private String createStatement;
	private String name;
	private List<Dependency> alldependencies = new ArrayList<>();
	private List<Dependency> dependencies = new ArrayList<>();
	private boolean isParsed = false;
	private List<List<Table>> circularDepPaths = new ArrayList<>();
	private boolean isSatisfied = false;

	

	/**
	 * Checks for dependencies.
	 *
	 * @return true, if successful
	 */
	public boolean hasDependencies() {
		return getDependencies() == null || getDependencies().isEmpty();
	}

	

	@Override
	public int compareTo(Table o) {
		return this.getName().compareTo(o.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Table) {
			return this.getName().equals(((Table) obj).getName());
		}
		return false;
	}

}
