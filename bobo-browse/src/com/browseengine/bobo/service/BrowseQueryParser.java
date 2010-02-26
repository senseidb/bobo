package com.browseengine.bobo.service;

import org.apache.lucene.search.DocIdSet;

/**
 * Builds a DocSet from an array of SelectioNodes
 */
public interface BrowseQueryParser {
	public static class SelectionNode
	{
		private String fieldName;
		private DocIdSet docSet;
		
		public SelectionNode()
		{	
		}
		
		public SelectionNode(String fieldName,DocIdSet docSet)
		{
			this.fieldName=fieldName;
			this.docSet=docSet;
		}
		
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}
		public DocIdSet getDocSet() {
			return docSet;
		}
		public void setDocSet(DocIdSet docSet) {
			this.docSet = docSet;
		}		
	}
	
	DocIdSet parse(SelectionNode[] selectionNodes,SelectionNode[] notSelectionNodes,int maxDoc);
}
