package main;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import java.io.File;


public class Args {
    @Parameter(names = "-apikey", converter = FileConverter.class)
    public File apikey;

    public class FileConverter implements IStringConverter<File> {
        @Override
        public File convert(String value) {
            return new File(value);
        }
    }
}





