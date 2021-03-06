<?php 
/*---------------------------------------------------------------------------*
 * controllers/answers_controller.php                                      *
 *                                                                           *
 * Controlls all web-end survey functions at the question level.  All        *
 * functions are ment to be AJAX.                                            *
 *---------------------------------------------------------------------------*/
/**
 * output answers
 * 
 * @author Tony Xiao
 */

class StatusChangesController extends AppController
{
	//for php4
	var $name = 'StatusChanges';
	var $components = array('Auth');

    function rest_index() {
        $this->autoRender = false;
        $this->header('Content-Type: application/json');
        $modelClass = $this->modelClass;
        // add any applicable filters
        $conditions = array();
        if (array_key_exists('filter', $this->params['url'])) {
            $filters = json_decode($this->params['url']['filter'], true);
            foreach ($filters as $filter)
                if (array_key_exists($filter['property'], $this->$modelClass->_schema))
                    $conditions[$modelClass.'.'.$filter['property']] = $filter['value'];
        }

        $models = $this->$modelClass->find('all', array(
            'recursive' => -1,
            'conditions' => $conditions,
            'order' => 'created DESC'
        ));
        e(json_encode(standardize($models, $modelClass)));
    }

    /** csv dump */
    function dump() {
        $this->autoRender = false;
        ini_set('max_execution_time', 600); //increase max_execution_time to 10 min if data set is very large
        
        $modelClass = $this->modelClass;
        $filename = $modelClass . "_dump_".date("Y.m.d").".csv";

        header('Content-type: application/csv');
        header('Content-Disposition: attachment; filename="'.$filename.'"');

        // custom stuff
        $csv_file = fopen('php://output', 'w');
        $headers = array('Time', 'Subject Id', 'Subject', 'Feature', 'Action');
        fputcsv($csv_file, $headers, ',', '"');

        $total = $this->$modelClass->find('count');
        $increment = 10000;

        for ($offset = 0; $offset<$total; $offset+=$increment) {
            $models = $this->$modelClass->find('all', array(
                'recursive' => 1,
                'order' => 'created DESC',
                'offset' => $offset,
                'limit' => $increment
            ));
            partial_dump($csv_file, $models);
        }
    
        fclose($csv_file);
    }
}

function partial_dump($csv_file, $models) {
    foreach($models as $item) {
        $row = array();
        $row[] = $item['StatusChange']['created']; // Time

        $row[] = $item['StatusChange']['subject_id']; // Subject Id
        $row[] = $item['Subject']['first_name'] ." ". $item['Subject']['last_name']; // Subject


        switch ($item['StatusChange']['feature']) { // Feature
            case 0:
                $row[] = 'GPS';
                break;
            case 1:
                $row[] = 'Call log';
                break;
            case 2:
                $row[] = 'Text log';
                break;
            case 3:
                $row[] = 'Surveys';
                break;
            default:
                $row[] = 'Undefined Feature';
        }

        switch ($item['StatusChange']['status']) { // Action
            case 0:
                $row[] = 'Disabling';
                break;
            case 1:
                $row[] = 'Enabling';
                break;
            default:
                $row[] = 'Undefined Status';
        }
        
        fputcsv($csv_file,$row,',','"');
    }
}

function standardize($models, $modelName) {
    // singular case
    if (array_key_exists($modelName, $models))
        return $models[$modelName];
    // array case
    $arr = array();
    foreach($models as $item) {
        $arr[] = $item[$modelName];
    }
    return $arr;
}

?>