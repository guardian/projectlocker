import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';
import PropTypes from 'prop-types';

class ProjectBoxMiddleComponentDiv extends React.Component {
    static propTypes = {
        location: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

    }

    render() {
        return (
            <div className="middle_project_box_div_version">
                <div className="middle_project_box_div_row">
                    <div className="middle_project_box_div_one">

                    </div>
                    <div className="middle_project_box_div_two">
                        <img src="/assets/images/project.png"/>
                    </div>
                    <div className="middle_project_box_div_three">
                        {this.props.title}
                    </div>
                    <div className="middle_project_box_div_four">
                        <a href="">KP-12345</a>
                    </div>
                </div>
                <div className="middle_project_box_div_row">
                    <div className="middle_project_box_div_one">
                        <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                    </div>
                    <div className="middle_project_box_div_two">
                        <img src="/assets/images/commission.png"/>
                    </div>
                    <div className="middle_project_box_div_three">
                        Place Holder Commission
                    </div>
                    <div className="middle_project_box_div_four">
                        <a href="">KP-54321</a>
                    </div>
                </div>
                <div className="middle_project_box_div_row">
                    <div className="middle_project_box_div_one">
                        <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                    </div>
                    <div className="middle_project_box_div_two_users">
                        <i className="fa fa-users" style={{color: "green"}}/>
                    </div>
                    <div className="middle_project_box_div_three">
                        Place Holder Working Group
                    </div>
                    <div className="middle_project_box_div_four">

                    </div>
                </div>
                <div className="middle_project_box_div_row">
                    <div className="middle_project_box_div_one">
                        <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                    </div>
                    <div className="middle_project_box_div_two_user">
                        <i className="fa fa-user" />
                    </div>
                    <div className="middle_project_box_div_three">
                        {this.props.user}
                    </div>
                    <div className="middle_project_box_div_four">

                    </div>
                </div>
             </div>
        );
    }
}

export default ProjectBoxMiddleComponentDiv;