/****************************************************************************
 * Copyright (C) 2017-2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of Epotheke
 *
 * Epotheke is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Epotheke is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Epotheke.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.demo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Florian Otto
 */
public class MainActivity extends FragmentActivity {

    private static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.base_layout);

        Button epo = findViewById(R.id.epo);
        if (epo != null) {
            epo.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setClass(MainActivity.this, EpothekeActivityImp.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            });
        }
    }

}
