/*
** AlacFile.java
**
** Copyright (c) 2011 Peter McQuillan
**
** All Rights Reserved.
**                       
** Distributed under the BSD Software License (see license.txt)  
**
*/
package com.beatofthedrum.alacdecoder;

public class AlacFile
{

	public byte[] input_buffer;
	public int[] channel_map;
	int ibIdx = 0;
	int input_buffer_bitaccumulator = 0; /* used so we can do arbitary
						bit reads */

	public int numchannels = 0;
	public int bytespersample_output = 0;

    LeadingZeros lz = new LeadingZeros();


    final int buffer_size = 16384;
    /* buffers */
	int[][] outputsamples_buffer;

	int[] uncompressed_bytes_buffer_a = null;
	int[] uncompressed_bytes_buffer_b = null;



	/* stuff from setinfo */
	public int setinfo_max_samples_per_frame = 0; // 0x1000 = 4096
	int bitspersample_input = 0; // 0x10
	int setinfo_rice_historymult = 0; // 0x28
	int setinfo_rice_initialhistory = 0; // 0x0a
	int setinfo_rice_kmodifier = 0; // 0x0e
	public int max_frame_bytes = 0;
	/* end setinfo stuff */

    int[] predictor_coef_table_a = new int[1024];
    int[] predictor_coef_table_b = null;
}