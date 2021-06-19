// ============================================================================
// Copyright (C) Schalk W. Cronje 2012
//
// This software is licensed under the Apche License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
// ============================================================================
package org.ysb33r.groovy.dsl.vfs.impl

import java.util.regex.Pattern;

import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileType
import org.apache.commons.vfs2.FileSelector
import org.apache.commons.vfs2.AllFileSelector
import org.ysb33r.groovy.dsl.vfs.FileActionException
import org.apache.commons.vfs2.Selectors

class CopyMoveOperations {

	static def friendlyURI( FileObject uri ) {
		return uri.name.friendlyURI
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @param smash
	 * @param overwrite
	 * @param filter Optional filter to apply. If the from type is a file, then the filter is ignored.
	 * @return
	 */
	static def copy( FileObject from,FileObject to,boolean smash,boolean overwrite,boolean recursive,filter=null ) {

		def fromType= from.type
		def toType= to.type
		assert fromType != FileType.FILE_OR_FOLDER
		
		if(!from.exists()) {
			throw new FileActionException("Source '${from.friendlyURI}' does not exist")
		}

		def selector= fromType == FileType.FILE ? Selectors.SELECT_ALL : _createSelector(filter)
		
		
		switch(fromType) {
			case FileType.FILE:
				_copyFromSourceFile(from,to,smash,_overwritePolicy(overwrite),selector)
				break					
			case FileType.FOLDER:
				_copyFromSourceDir(from,to,smash,_overwritePolicy(overwrite),recursive,selector)
				break
		}		
		
	}

	/** Creates a VFS selector from a passed in filter
	 * @todo NEEDS A LOT OF WORK
	 * @return
	 */
	static def _createSelector(filter) {
		
		def selector
		switch (filter) {
			case null:
				selector=Selectors.SELECT_ALL
				break				
			case Pattern :
				assert false
			/*
				selector = [
					'includeFile' : { fsi -> fsi.file.name.baseName ==~ properties.filter },
					'traverseDescendents' : traverse
				]
			*/
				break
			case FileSelector:
				selector=filter
				break
			case Closure:
				assert false
				/*
				selector = [
					'includeFile' : { fsi -> properties.filter.call(fsi) },
					'traverseDescendents' : traverse
				]
				*/
				break
			default:
				assert false
				/*
				selector = [
					'includeFile' : { fsi -> fsi.file.name.baseName ==~ /"${properties.filter.toString()}"/ },
					'traverseDescendents' : traverse
				]
				*/
		}
	}
	
	/** Implements copying from a source file
	 * 
	 */
	private static def _copyFromSourceFile(FileObject from,FileObject to,boolean smash,Closure overwrite,FileSelector selector) {
		def toType= to.type
		assert toType != FileType.FILE_OR_FOLDER

		FileObject target
		switch(toType) { 
			case FileType.FOLDER:					
				if(!smash) {
					target=to.resolveFile(from.name.baseName)
					if(target.type == FileType.FOLDER) {
						throw new FileActionException("Destination directory '${this.friendlyURI(to)}' contains directory with the same name as the source file '${from.name.baseName}'") 
					} else if (target.type == FileType.FILE && target.exists() && !overwrite(from,target) ) {
						throw new FileActionException("'${this.friendlyURI(target)}' exists and overwrite mode is not set")
					}					
				} else {
					target=to
				}

				break
			case FileType.FILE:
				if(to.exists() && !overwrite(from,to)) {
					throw new FileActionException("'${this.friendlyURI(to)}' exists and overwrite mode is not set")							
				}
			case FileType.IMAGINARY:
				target=to
				break
			default:
				assert false,"Should never get here"
		}

		target.copyFrom(from,selector)
		return
	}	

	/** Implements copying from a source directory
	 * 
	 */
	private static def _copyFromSourceDir(FileObject from,FileObject to,boolean smash,Closure overwrite,boolean recursive,FileSelector selector) {
		def toType= to.type
		FileObject target
		
		if(toType == FileType.FILE && !smash) {
			throw new FileActionException("${this.friendlyURI(from)} is a directory, ${this.friendlyURI(to)} is a file, and smash is not set")
		}
		
		switch(toType) {
			case FileType.FILE:
				if(!smash) {
					throw new FileActionException("${this.friendlyURI(from)} is a directory, ${this.friendlyURI(to)} is a file, and smash is not set")				
				}
				to.copyFrom(from,selector)
				break
			case FileType.FOLDER:
				if(smash) {					
					def original=to.parent.resolveFile('$'*10+"${to.name.baseName}"+'$'*10)
					target=to
					to.moveTo(original)
					try {
						target.copyFrom(from,selector)						
					}
					finally {
						original.delete(Selectors.SELECT_ALL)
					}
					return 
				} else if (recursive) {
					target=to.resolveFile(from.name.baseName) 
					if(target.exists()) {
						_recursiveDirCopyNoSmash(from,target,selector,overwrite)
					} else {
						target.copyFrom(from,selector)
					}
				} else {
					throw new FileActionException( "Attempt to copy from folder '${friendlyURI(from)}' to folder '${friendlyURI(to)}', but recursive and smash are not set")
				}
				break
			case FileType.IMAGINARY:
				if(recursive) {
					to.copyFrom(from,selector)
				} else {
					throw new FileActionException( "Attemping to copy ${friendlyURI(from)} to ${friendlyURI(to)}, but recursive is off" )
				}
				break
			default:
				assert false,"Should never get here"			
		}			
	}
	
	/** Performs a recursive directory to directory copy, applying an overwrite
	 * policy along the way. Only the descendants of the source directory are 
	 * copied. If the overwrite closure returns 'false' at any point it, 
	 * a FileActionException will be raised.
	 * 
	 * @param from Source directory to copy from.
	 * @param to Directory to copy to
	 * @param selector A selector to choose which children from the source to copy
	 * @param overwrite Closure that returns true or false whether a source should overwrite a target
	 * @return
	 */
	private static def _recursiveDirCopyNoSmash(FileObject from,FileObject to,FileSelector selector,Closure overwrite) {
		FileSelector combinedSelector=[ 
			includeFile : {
				if (!selector.includeFile(it)) {
					return false
				}
				def src= from.name.getRelativeName(it.file.name)
				def target=to.resolveFile(src)

				
				if(target.exists()) {
					if(!overwrite(it.file,target)) {
						throw new FileActionException("Overwriting existing target '${friendlyURI(to)}' is not allowed")
					} else if(target.type == FileType.FOLDER) {
						throw new FileActionException("Replacing existing target folder '${friendlyURI(to)}' with a file is not allowed")
					}
				}
				return true
			},
			traverseDescendents : {selector.traverseDescendents(it)}
		] as FileSelector
	
		from.children.each {
			def nextTarget=to.resolveFile(it.name.baseName)
			try {
				nextTarget.copyFrom(it,combinedSelector)
			} catch(Exception e) {
				throw new FileActionException("Failing to create '${friendlyURI(nextTarget)}'. Maybe overwrite was not allowed.",nextTarget,e)
			}
		}
	}
	
	/** Returns a closure which can be prompted on a file-by-file basis about overwriting
	 * 
	 * @param overwrite
	 * @return
	 */
	private static def _overwritePolicy(overwrite) {
		switch(overwrite) {
			case true:
				return {f,t->true}
			case Closure:
				return overwrite
			case false:
				return {f,t->false}
			default:
				return {f,t->false}
		}
	}
}
