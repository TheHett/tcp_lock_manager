<?php

class LockManager
{

    public $server = 'tcp://127.0.0.1';
    public $connectTimeout = 0.2;

    protected $_buffer;
    protected $_socket;

    public function init()
    {
        $this->_socket = @fsockopen($this->server, 1234, $errno, $errstr, $this->connectTimeout);
        if ($this->_socket === false) {
            //@todo logging
        }
    }

    protected function read_line()
    {

        while (strpos($this->_buffer, "\n") === false)
            $this->_buffer .= fread($this->_socket, 64);

        $lineEnd = strpos($this->_buffer, "\n");
        $line = substr($this->_buffer, 0, $lineEnd);

        $this->_buffer = substr($this->_buffer, $lineEnd + 1);

        return $line;
    }

    protected function write_line($line)
    {
        fwrite($this->_socket, $line . "\n");
    }

    public function acquire($name, $timeoutMs = 0)
    {
        if ($this->_socket === false) {
            return true;
        }
        $this->write_line("LOCK {$name} {$timeoutMs}");
        $response = $this->read_line();

        switch ($response) {
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
        if ($this->_socket === false) {
            return;
        }

        $this->write_line("RELEASE {$name}");
    }

    public function ilock($name)
    {
        if($this->_socket === false) {
            return false;
        }

        $this->write_line("ILOCK {$name}");
        $response = $this->read_line();
        switch ($response) {
            case "YES":
                return true;
            case "NO":
                return false;
            default:
                //@todo log unknown response
                return false;
        }
    }

}