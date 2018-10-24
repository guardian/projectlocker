import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';

class ProjectBoxRightComponent extends React.Component {
    static ITEM_LIMIT=50;

    constructor(props){
        super(props);

    }

    render() {
        return (
            <div className="right_project_box">
                <div className="right_project_box_top">
                    Open
                </div>
                <div className="right_project_box_middle">
                    <img src="/assets/images/folder.png"/>
                </div>
                <div className="right_project_box_bottom">
                    <img src="/assets/images/premiere_pro.png"/>
                </div>
            </div>
        );
    }
}

export default ProjectBoxRightComponent;