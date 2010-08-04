package com.browseengine.bobo.gwt.widgets;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TagCloudFacetView extends AbstractFacetView {
	private final Label _headingLabel;
	private final FlowPanel _titlePane;
	private final FlowPanel _selTagsPane;
	private final FlowPanel _mainPane;
    
	public @UiConstructor TagCloudFacetView(String name){
		super(name);
		VerticalPanel topPane = new VerticalPanel();
		_headingLabel = new Label();
		_headingLabel.setText(name);
		//_headingLabel.setStyleName(getBoboStyleName(FACET_HEADING_STYLE));
		
		_titlePane = new FlowPanel();
		_titlePane.add(_headingLabel);
		_selTagsPane = new FlowPanel();
		_titlePane.add(_selTagsPane);
		
		_mainPane = new FlowPanel();
		
		topPane.add(_titlePane);
		topPane.add(_mainPane);
		
		initWidget(topPane);
	}
	
	@Override
	public void updateSelections(List<FacetValue> selections) {
		_selTagsPane.clear();
		_mainPane.clear();
		int totalCount = 0;
		if (selections!=null){
			for (FacetValue facet : selections){
				totalCount+=facet.getCount();
			}
			Collections.sort(selections, new Comparator<FacetValue>(){
				public int compare(FacetValue o1, FacetValue o2) {
					return o1.getValue().compareTo(o2.getValue());
				}
			});
			for (FacetValue facet : selections){
				String val = facet.getValue();
				if (!facet.isSelected()){
				  int size = facet.getCount()*10/totalCount;
				  Anchor anchor = new Anchor();
				  Label label = new Label(" ");
				  _mainPane.add(label);
				  _mainPane.add(anchor);
				  anchor.setText(val);
				  anchor.setStyleName(val+"-"+size);
				  //anchor.addMouseOverHandler(handler);
				  anchor.addClickHandler(new ClickHandler(){
					public void onClick(ClickEvent event) {
						Anchor src = (Anchor)event.getSource();
						fireFacetSelectionEvent(src.getText(),true);
					}
				  });
				}
				else{
					Anchor anchor = new Anchor();
					  _selTagsPane.add(anchor);
					  anchor.setText(val);
					  anchor.setStyleName(getStyleName());
					  anchor.addClickHandler(new ClickHandler(){

						public void onClick(ClickEvent event) {
							Anchor src = (Anchor)event.getSource();
							fireFacetSelectionEvent(src.getText(),false);
						}
						  
					  });
				}
			}
		}
	}
}
