import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';
import PropTypes from 'prop-types';

class ProjectBoxRightComponent extends React.Component {
    static propTypes = {
        location: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

    }

    render() {
        if (this.props.size == 0) {
            return (
                <div className="right_project_box_small">
                    <div className="right_project_box_top_small">
                        Open
                    </div>
                    <div className="right_project_box_middle_small">
                        <img width="60" src="/assets/images/folder.png"/>
                    </div>
                    <div className="right_project_box_bottom_small">
                        <img width="60" src="/assets/images/premiere_pro.png"/>
                    </div>
                </div>
            );
        } else {
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
}

export default ProjectBoxRightComponent;