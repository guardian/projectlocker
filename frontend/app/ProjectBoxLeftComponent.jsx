import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';

class ProjectBoxLeftComponent extends React.Component {
    static ITEM_LIMIT=50;

    constructor(props){
        super(props);

    }

    render() {
        return (
            <div className="left_project_box">
                <img src="/assets/images/place_holder.jpg"/>

            </div>
        );
    }
}

export default ProjectBoxLeftComponent;