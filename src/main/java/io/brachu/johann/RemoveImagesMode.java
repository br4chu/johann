package io.brachu.johann;

enum RemoveImagesMode {

    NONE {
        @Override
        public String toCmd() {
            return "";
        }
    },
    LOCAL {
        @Override
        public String toCmd() {
            return "local";
        }
    },
    ALL {
        @Override
        public String toCmd() {
            return "all";
        }
    };

    public abstract String toCmd();
}
