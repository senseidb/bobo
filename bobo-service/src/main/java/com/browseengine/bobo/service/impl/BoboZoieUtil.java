package com.browseengine.bobo.service.impl;

import java.io.IOException;
import java.util.List;

import proj.zoie.api.ZoieIndexReader;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.MultiBoboBrowser;

public class BoboZoieUtil {
	public static Browsable buildBrowsableFromZoieReaders(
			List<ZoieIndexReader<BoboIndexReader>> readerList)
			throws IOException {
		List<BoboIndexReader> boboReaderList = ZoieIndexReader
				.extractDecoratedReaders(readerList);
		return buildBrowsableFromBoboReaders(boboReaderList);
	}

	public static Browsable buildBrowsableFromBoboReaders(
			List<BoboIndexReader> readerList) throws IOException {
		Browsable[] subBrowsers = BoboBrowser.createBrowsables(readerList);
		return new MultiBoboBrowser(subBrowsers);
	}
}
