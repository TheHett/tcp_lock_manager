<?php

class LockManager {

    public $hostname = 'tcp://127.0.0.1';

    protected $_buffer;
    protected $_socket;

    public function __construct($connectTimeout = 0.2)
    {
        $this->_socket = @fsockopen($this->hostname, 1234, $errno, $errstr, $connectTimeout);
        if($this->_socket === false) {
            //@todo error logging
        }
    }

    protected function read_line() {

        while (strpos($this->_buffer, "\n") === false)
            $this->_buffer .= fread($this->_socket, 64);

        $lineEnd = strpos($this->_buffer, "\n");
        $line = substr($this->_buffer, 0, $lineEnd);
        $this->_buffer = substr($this->_buffer, $lineEnd);

        return $line;
    }

    protected function write_line($line) {
        fwrite($this->_socket, $line . "\n");
    }

    public function acquire($name, $timeoutMs = 0)
    {
        if($this->_socket === false) {
            return true;
        }
        $this->write_line("LOCK {$name} {$timeoutMs}");
        $response = $this->read_line();
        switch($response) {
            case 'SUCCESS':
                return true;
            case 'BUSY':
                return false;
            default:
                //@todo log unknown response
                return false;
        }
    }

    public function release($name)
    {
        if($this->_socket === false) {
            return;
        }

        $this->write_line("RELEASE {$name}");
    }


}