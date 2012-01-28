
/**
 * 
 * @param root
 * @return
 */
def getSnapshotsDirs(File root){
	if(!root.exists()){
		println "no such directory (${root})"
		return [];
	}
	List<File> dirs = []
	root.eachDirRecurse {
		File dir = it
		if(dir.name.indexOf("-SNAPSHOT") != -1) {
			dirs.add(dir)
		}
	}
	return dirs;
}

def getGarvageList(File parent, int minNum){
	def removeList = []
	getSnapshotsDirs(parent).each { File dir ->
		File xml = new File(dir, "maven-metadata.xml")
		if(xml.exists()){
			def metadata = new XmlParser().parse(xml)
			int buildNumber = Integer.parseInt(metadata.versioning.snapshot.buildNumber.text())
			dir.eachFile{
				(it.name =~ /-(\d{8}\.\d{6})-(\d+)\./).each{ll, ts, bn ->
					if( buildNumber - Integer.parseInt(bn) >= minNum){
						removeList.add(it)
					}
				}
			}
		}
	}
	return removeList
}

def cmd = this.class.name + '.groovy'
CliBuilder cli = new CliBuilder(usage:"${cmd} [options] [root-directory]")
cli.h(longOpt:'help','display this message.')
cli.k(longOpt:'keep', 'minimun number of snapshots to keep for one GAV.', args:1, argName:'number')
cli.v(longOpt:'verbose', 'display file name to process.')
cli.o(longOpt:'output-list', args:1, argName:'file','output list of all files to be removed.')
cli.t(longOpt:'test', 'test mode.')
def options = cli.parse(args)

if(options.h){
	cli.usage()
	return
}

def minNum = options.k ? options.k.toInteger() : 1 
File parent = new File(options.arguments()[0] ?: "./")

def list = getGarvageList(parent, minNum)
list.each {File f ->
	if(!options.t){
		f.delete()
	}
	if(options.v){
		println f.name
	}
}

if(options.o){
	new File(options.o).withWriter {writer ->
		list.each {File file ->
			writer << file.absolutePath + '\n'
		}
	}
}

if(!options.t){
	println "${list.size} files are removed."
}else{
	println "${list.size} files are hit."
}