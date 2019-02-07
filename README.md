# reorder-sql-ddl
Simple tool to reorder sql ddls so that dependency does not create issues

When we are importing DB but due to foreign keys and random order of exported files, mysql server does not allow to import tables.

This can detect circuler dependency too.

# Usage

## Create structure file
Create a sql file with all the create statements, can use mysql dump to create structure only using following command
```
mysqldump --user=root -p --default-character-set=utf8 --single-transaction=TRUE --routines --events --no-data "DB_NAME" > dbbackup/prefix-$(date +"%Y-%m-%d")-structure.sql
```
## Run
Change file name and path in ReOrder.java file
Run as simple java file

## Output
* It will show all the tables with dependencies.
* If any of table has circular dependency then it will show that in console along with the path from where circular dependency created
	* It can detect circular any level or transitive circular dependency
* If no circular dependency found, then it will create a file named "reordered.sql" with all the create statements
* It will log a mysqldump command which you can use to export data in the same order of dependencies so that it does not create any issue

# Disclaimer
Created this simple tool to have quick solution. Don't claim to provide perfect output. 
Create a pull request if you find any improvement

# License
Do whatever you want to do with this.
 