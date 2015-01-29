/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2014 TIBCO Software Inc. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.components.barcode4j;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRComponentElement;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRRuntimeException;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.Renderable;
import net.sf.jasperreports.engine.RenderableUtil;
import net.sf.jasperreports.engine.type.ImageTypeEnum;
import net.sf.jasperreports.engine.type.OnErrorTypeEnum;
import net.sf.jasperreports.engine.util.JRColorUtil;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * 
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 */
public class QRCodeRasterizedImageProducer implements QRCodeImageProducer
{
	
	public Renderable createImage(
		JasperReportsContext jasperReportsContext,
		JRComponentElement componentElement, 
		QRCodeBean qrCodeBean, 
		String message
		)
	{
		QRCodeWriter writer = new QRCodeWriter();

		Map<EncodeHintType,Object> hints = new HashMap<EncodeHintType,Object>();
		hints.put(EncodeHintType.CHARACTER_SET, QRCodeComponent.PROPERTY_DEFAULT_ENCODING);
		hints.put(EncodeHintType.ERROR_CORRECTION, qrCodeBean.getErrorCorrectionLevel().getErrorCorrectionLevel());
		hints.put(EncodeHintType.MARGIN,  qrCodeBean.getMargin());

		int resolution = JRPropertiesUtil.getInstance(jasperReportsContext).getIntegerProperty(
				componentElement, BarcodeRasterizedImageProducer.PROPERTY_RESOLUTION, 300);

		try
		{
			BitMatrix matrix = 
				writer.encode(
					message, 
					BarcodeFormat.QR_CODE, 
//					(int)((72f / 2.54f) * componentElement.getWidth()), 
//					(int)((72f / 2.54f) * componentElement.getHeight()), 
					(int)((resolution / 72f) * componentElement.getWidth()), 
					(int)((resolution / 72f) * componentElement.getHeight()), 
					hints
					);
			MatrixToImageConfig config = 
				new MatrixToImageConfig(
					JRColorUtil.getOpaqueArgb(componentElement.getForecolor(), Color.BLACK),
					JRColorUtil.getOpaqueArgb(componentElement.getBackcolor(), Color.WHITE)
					);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);
			return RenderableUtil.getInstance(jasperReportsContext).getRenderable(image, ImageTypeEnum.PNG, OnErrorTypeEnum.ERROR);
		}
		catch (WriterException e)
		{
			throw new JRRuntimeException(e);
		}
		catch (JRException e)
		{
			throw new JRRuntimeException(e);
		}
	}

}
