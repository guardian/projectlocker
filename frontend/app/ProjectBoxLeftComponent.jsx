import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';
import PropTypes from 'prop-types';

class ProjectBoxLeftComponent extends React.Component {
    static propTypes = {
        location: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

    }

    render() {
        if (this.props.size == 0) {

            return (
                <div className="left_project_box_small">
                    <img height="136" src="/assets/images/place_holder.jpg"/>
                </div>
            );

        } else {

            return (
                <div className="left_project_box">
                    <img src="/assets/images/place_holder.jpg"/>
                </div>
            );
        }
    }
}

export default ProjectBoxLeftComponent;