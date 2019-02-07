
/**
 * 
 */

package ddl.reorder.mysql;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ddl.reorder.mysql.model.Dependency;
import ddl.reorder.mysql.model.Table;

/**
 * @author Akshay Vadher
 * The Class re order.
 */
public class ReOrder {

	Pattern tableNamePattern = Pattern.compile("CREATE TABLE (.+?) \\(");
	Pattern keys = Pattern.compile("CONSTRAINT (.+?) FOREIGN KEY \\((.+?)\\) REFERENCES (.+?) \\((.+?)\\) ON");

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
//		if (args == null || args.length == 0) {
//			System.err.println("No input file. Pass it as 'java Organise <filename>'");
//		}
//		String srcFileName = args[0];
		String srcFileName = "path\\to\\file\\all-tables-create.sql";
		ReOrder o = new ReOrder();
		String fileContent = o.readFile(srcFileName);
		List<Table> tables = o.parseFile(fileContent);
		List<Dependency> allDependencies = new ArrayList<>();
		o.parseDependencyTables(tables, allDependencies);
		List<Table> tablesWithCircularDep = new ArrayList<>();
		List<Table> orderedTables = o.organise(tables, tablesWithCircularDep);

		o.printTables(orderedTables, tablesWithCircularDep);

	}

	public String readFile(String srcFileName) {
		StringBuffer sb = new StringBuffer();
		try (BufferedReader br = new BufferedReader(new FileReader(srcFileName))) {
			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null) {
				sb.append(sCurrentLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public List<Table> parseFile(String fileContent) {
		List<Table> parsedTables = new ArrayList<>();
		String[] tableContents = fileContent.split(";");
		for (String tableString : tableContents) {
			Table table = new Table();
			tableString = tableString.replaceAll("`|'|\"", "");
			table.setCreateStatement(tableString);

			Matcher m = tableNamePattern.matcher(tableString);
			if (m.find()) {
				String tableName = m.group(1);
				table.setName(tableName);
				parsedTables.add(table);
			} else {
				System.out.println("Not found");
			}
			Matcher m2 = keys.matcher(tableString);
			while (m2.find()) {
				Dependency dep = new Dependency();
				String keyName = m2.group(1);
				dep.setKeyName(keyName);
				String srcColumn = m2.group(2);
				dep.setColumn(srcColumn);
				String depOnTable = m2.group(3);
				dep.setDepOnTableName(depOnTable);
				String depOnColumn = m2.group(4);
				dep.setDepOnTableColumn(depOnColumn);
				dep.setSrcTable(table);
				if (dep.getSrcTable().getName().equals(depOnTable)) {
					dep.setSatisfied(true);
				}

				// ignoring self join for now.
				if (!dep.getSrcTable().getName().equals(depOnTable)) {
					table.getDependencies().add(dep);
					table.getAlldependencies().add(dep);
				}
			}
		}
		return parsedTables;
	}

	public void parseDependencyTables(List<Table> tables, List<Dependency> allDependencies) {
		tables.forEach(table -> table.getDependencies().forEach(dep -> {
			allDependencies.add(dep);

			String depOnTableName = dep.getDepOnTableName();
			Table deptable = tables.stream().filter(tab -> tab.getName().equals(depOnTableName)).findAny()
					.orElseGet(null);
			if (deptable == null) {
				System.out.println("NO " + depOnTableName + " for " + table.getName());
			}
			dep.setDepOnTable(deptable);
		}));
	}

	private List<Table> organise(List<Table> tables, List<Table> tablesWithCircularDep) {
		List<Table> orderedTables = new ArrayList<>();
		Long notSatisfiedCount = Long.valueOf(String.valueOf(tables.size()));
		while (notSatisfiedCount > 0) {
			tables.stream().filter(tab -> !tab.isSatisfied()).forEach(tab -> {
				List<Table> allSrcTables = new ArrayList<>();
				findAndInsert(tables, tab, orderedTables, allSrcTables, tablesWithCircularDep);
			});
			notSatisfiedCount = tables.stream().filter(tab -> !tab.isSatisfied()).count();
		}
		return orderedTables;
	}

	private void findAndInsert(List<Table> allTables, Table srcTab, List<Table> orderedTables, List<Table> allSrcTables,
			List<Table> tablesWithCircularDep) {
		if (srcTab.getDependencies().isEmpty()
				|| srcTab.getDependencies().stream().filter(dep -> !dep.isSatisfied()).count() == 0) {
			if (!orderedTables.contains(srcTab)) {
				orderedTables.add(srcTab);
				srcTab.setSatisfied(true);
				removeDepFromPendingDeps(allTables, srcTab);
			}
		} else {
			srcTab.getDependencies().stream().filter(dep -> dep.getDepOnTable() != null && !dep.isSatisfied())
					.map(Dependency::getDepOnTable).forEach(depTab -> {
						// find if any circ dep
						List<Table> allSourceTablesForThisDep = new ArrayList<>();
						allSourceTablesForThisDep.addAll(allSrcTables);
						// if the last table is same as this then remove them.
						// This happens on self referenced tables
//						if (!allSourceTablesForThisDep.isEmpty()
//								&& allSourceTablesForThisDep.get(allSourceTablesForThisDep.size() - 1).equals(depTab)) {
//							allSourceTablesForThisDep.remove(allSourceTablesForThisDep.size() - 1);
//						}
						if (allSourceTablesForThisDep.contains(depTab)) {
							tablesWithCircularDep.add(depTab);
							depTab.setSatisfied(true);
							removeDepFromPendingDeps(allTables, depTab);
							depTab.getCircularDepPaths().add(allSrcTables);
						} else {
							// adding this later as this can be the case of self dependency
							allSourceTablesForThisDep.add(srcTab);
							findAndInsert(allTables, depTab, orderedTables, allSourceTablesForThisDep,
									tablesWithCircularDep);
						}
					});
		}
	}

	public void removeDepFromPendingDeps(List<Table> allTables, Table depTable) {
		allTables.forEach(tab -> tab.getDependencies().stream()
				.filter(dep -> dep.getDepOnTable() != null && dep.getDepOnTable().equals(depTable))
				.forEach(dep -> dep.setSatisfied(true)));
	}

	public String printCircularDepsPath(Table table) {
		return table.getCircularDepPaths().stream()
				.map(path -> path.stream().map(Table::getName).collect(Collectors.joining(" --> ")))
				.collect(Collectors.joining("\n\t"));
	}

	public void printTables(List<Table> tables, List<Table> tablesWithCircularDep) {
		System.out.println("-------------");
		System.out.println("These tables have circular deps");
		System.out.println("-------------");
		tablesWithCircularDep.stream().forEach(tab -> {
			System.out.println(tab.getName());
			tab.getAlldependencies().forEach(dep -> System.out.println("\t" + dep.getDepOnTableName()));
			System.out.println("\t -- Circular via\n\t" + printCircularDepsPath(tab));
		});
		if(tablesWithCircularDep.isEmpty()) {
			System.out.println("Bravo... No circular depd.");
		}
		System.out.println("-------------");
		System.out.println("\n-------------");
		System.out.println("Ordered");
		System.out.println("-------------");
		tables.forEach(table -> {
			System.out.println(table.getName());
			table.getAlldependencies().forEach(dep -> System.out.println("\t" + dep.getDepOnTableName()));
			System.out.println("-------------");
		});
		
		if (tablesWithCircularDep.isEmpty()) {
			Path file = Paths.get("reordered.sql");
			try {
				Files.write(file, tables.stream().map(Table::getCreateStatement).collect(Collectors.joining(";\n"))
						.getBytes());
			} catch (IOException e) {
				System.err.println("Could not write file");
			}
		}
	}
}